package com.ccloud.framework.ratelimiter.core.keyresolver;

import cn.hutool.crypto.digest.DigestUtil;
import com.ccloud.framework.ratelimiter.core.annotation.RateLimiter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Arrays;

/**
 * 按方法 + 入参限流
 */
public class DefaultRateLimiterKeyResolver implements RateLimiterKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
        String methodName = ((MethodSignature) joinPoint.getSignature()).toShortString();
        return DigestUtil.md5Hex(methodName + Arrays.toString(joinPoint.getArgs()));
    }
}
