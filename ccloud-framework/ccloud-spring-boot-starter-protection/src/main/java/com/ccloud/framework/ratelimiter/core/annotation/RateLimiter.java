package com.ccloud.framework.ratelimiter.core.annotation;

import com.ccloud.framework.ratelimiter.core.keyresolver.DefaultRateLimiterKeyResolver;
import com.ccloud.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流：在 time 窗口内最多允许 count 次调用
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RateLimiter {

    int time() default 1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    int count() default 100;

    String message() default "请求过于频繁，请稍后重试";

    Class<? extends RateLimiterKeyResolver> keyResolver() default DefaultRateLimiterKeyResolver.class;
}
