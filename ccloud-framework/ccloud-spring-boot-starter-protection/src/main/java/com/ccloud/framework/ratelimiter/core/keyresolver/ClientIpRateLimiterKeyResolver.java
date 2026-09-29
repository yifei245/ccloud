package com.ccloud.framework.ratelimiter.core.keyresolver;

import cn.hutool.crypto.digest.DigestUtil;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.ratelimiter.core.annotation.RateLimiter;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * 按客户端 IP 限流。用于登录、发短信这类未登录即可调用的接口。
 */
public class ClientIpRateLimiterKeyResolver implements RateLimiterKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
        String methodName = ((MethodSignature) joinPoint.getSignature()).toShortString();
        String clientIp = ServletUtils.getClientIP(WebFrameworkUtils.getRequest());
        return DigestUtil.md5Hex(methodName + "-" + clientIp);
    }
}
