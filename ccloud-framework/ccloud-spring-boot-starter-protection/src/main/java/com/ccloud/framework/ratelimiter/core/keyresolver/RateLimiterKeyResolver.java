package com.ccloud.framework.ratelimiter.core.keyresolver;

import com.ccloud.framework.ratelimiter.core.annotation.RateLimiter;
import org.aspectj.lang.JoinPoint;

/**
 * 限流键生成策略。键决定了"按什么维度限流"。
 */
public interface RateLimiterKeyResolver {

    String resolver(JoinPoint joinPoint, RateLimiter rateLimiter);
}
