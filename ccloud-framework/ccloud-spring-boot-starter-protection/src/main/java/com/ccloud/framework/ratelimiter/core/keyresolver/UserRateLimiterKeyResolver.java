package com.ccloud.framework.ratelimiter.core.keyresolver;

import cn.hutool.crypto.digest.DigestUtil;
import com.ccloud.framework.ratelimiter.core.annotation.RateLimiter;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * 按登录用户限流
 */
public class UserRateLimiterKeyResolver implements RateLimiterKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
        String methodName = ((MethodSignature) joinPoint.getSignature()).toShortString();
        return DigestUtil.md5Hex(methodName + "-" + WebFrameworkUtils.getLoginUserId());
    }
}
