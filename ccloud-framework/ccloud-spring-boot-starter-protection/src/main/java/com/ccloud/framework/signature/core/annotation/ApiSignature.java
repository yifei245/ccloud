package com.ccloud.framework.signature.core.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 开放 API 签名校验。
 *
 * <p>请求需携带四个头：{@code X-App-Key}、{@code X-Timestamp}、
 * {@code X-Nonce}、{@code X-Sign}。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ApiSignature {

    /**
     * nonce 的保留时长，同时也是签名的有效期。
     *
     * <p>两者必须一致：nonce 若比签名先过期，攻击者就能在签名仍有效的窗口内
     * 用同一个 nonce 重放。
     */
    int timeout() default 5;

    TimeUnit timeUnit() default TimeUnit.MINUTES;

    String message() default "签名校验失败";
}
