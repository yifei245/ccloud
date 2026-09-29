package com.ccloud.framework.idempotent.core.annotation;

import com.ccloud.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import com.ccloud.framework.idempotent.core.keyresolver.DefaultIdempotentKeyResolver;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 幂等：在有效期内，相同 key 的重复请求只有第一次能进入方法体。
 *
 * <p>典型场景是用户连点提交按钮、网络重试导致的重复下单。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Idempotent {

    int timeout() default 1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    String message() default "操作太频繁，请稍后重试";

    Class<? extends IdempotentKeyResolver> keyResolver() default DefaultIdempotentKeyResolver.class;

    /**
     * 业务执行抛异常时是否释放幂等键。
     *
     * <p>默认释放：失败的操作没有产生副作用，理应允许立刻重试。
     * 若下游可能"已执行成功但响应失败"，应设为 false 让键保留到过期。
     */
    boolean deleteWhenException() default true;
}
