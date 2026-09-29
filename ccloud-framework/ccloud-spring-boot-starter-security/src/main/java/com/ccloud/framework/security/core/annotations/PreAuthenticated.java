package com.ccloud.framework.security.core.annotations;

import java.lang.annotation.*;

/**
 * 标记接口需要登录但不校验具体权限。
 *
 * <p>用于"任何登录用户都能访问"的接口，比写
 * {@code @PreAuthorize("isAuthenticated()")} 更醒目。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface PreAuthenticated {
}
