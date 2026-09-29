package com.ccloud.framework.idempotent.core.aop;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.idempotent.core.annotation.Idempotent;
import com.ccloud.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import com.ccloud.framework.idempotent.core.redis.IdempotentRedisDAO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link Idempotent} 的切面实现
 */
@Aspect
@Slf4j
public class IdempotentAspect {

    private final Map<Class<? extends IdempotentKeyResolver>, IdempotentKeyResolver> keyResolvers;

    private final IdempotentRedisDAO idempotentRedisDAO;

    public IdempotentAspect(List<IdempotentKeyResolver> keyResolvers, IdempotentRedisDAO idempotentRedisDAO) {
        this.keyResolvers = keyResolvers.stream()
                .collect(Collectors.toMap(IdempotentKeyResolver::getClass, Function.identity()));
        this.idempotentRedisDAO = idempotentRedisDAO;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        IdempotentKeyResolver keyResolver = keyResolvers.get(idempotent.keyResolver());
        if (keyResolver == null) {
            throw new IllegalStateException("找不到幂等键生成器 " + idempotent.keyResolver().getName()
                    + "，请将其注册为 Bean");
        }
        String key = keyResolver.resolver((JoinPoint) joinPoint, idempotent);
        Duration duration = Duration.ofMillis(idempotent.timeUnit().toMillis(idempotent.timeout()));

        // setIfAbsent 是原子操作，并发下只有一个请求能抢到
        if (!Boolean.TRUE.equals(idempotentRedisDAO.setIfAbsent(key, duration))) {
            log.info("[around] 重复请求被拦截，key={}", key);
            throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), idempotent.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // 失败的操作没有产生副作用，释放键让用户可以立刻重试
            if (idempotent.deleteWhenException()) {
                idempotentRedisDAO.delete(key);
            }
            throw throwable;
        }
    }
}
