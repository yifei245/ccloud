package com.ccloud.framework.ratelimiter.core.aop;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.ratelimiter.core.annotation.RateLimiter;
import com.ccloud.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import com.ccloud.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
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
 * {@link RateLimiter} 的切面实现
 */
@Aspect
@Slf4j
public class RateLimiterAspect {

    private final Map<Class<? extends RateLimiterKeyResolver>, RateLimiterKeyResolver> keyResolvers;

    private final RateLimiterRedisDAO rateLimiterRedisDAO;

    public RateLimiterAspect(List<RateLimiterKeyResolver> keyResolvers, RateLimiterRedisDAO rateLimiterRedisDAO) {
        this.keyResolvers = keyResolvers.stream()
                .collect(Collectors.toMap(RateLimiterKeyResolver::getClass, Function.identity()));
        this.rateLimiterRedisDAO = rateLimiterRedisDAO;
    }

    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) throws Throwable {
        RateLimiterKeyResolver keyResolver = keyResolvers.get(rateLimiter.keyResolver());
        if (keyResolver == null) {
            throw new IllegalStateException("找不到限流键生成器 " + rateLimiter.keyResolver().getName()
                    + "，请将其注册为 Bean");
        }
        String key = keyResolver.resolver((JoinPoint) joinPoint, rateLimiter);
        Duration time = Duration.ofMillis(rateLimiter.timeUnit().toMillis(rateLimiter.time()));

        if (!rateLimiterRedisDAO.tryAcquire(key, rateLimiter.count(), time)) {
            log.info("[around] 触发限流，key={}", key);
            throw new ServiceException(GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getCode(), rateLimiter.message());
        }
        return joinPoint.proceed();
    }
}
