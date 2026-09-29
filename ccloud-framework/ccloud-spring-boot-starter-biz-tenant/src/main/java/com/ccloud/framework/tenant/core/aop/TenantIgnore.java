package com.ccloud.framework.tenant.core.aop;

import java.lang.annotation.*;

/**
 * 标记方法执行期间忽略租户隔离。
 *
 * <p>使用前请确认：该方法确实是系统级操作。用它来"绕过报错"是危险的，
 * 会让本应隔离的查询变成全租户可见。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TenantIgnore {

    /**
     * 忽略的原因，强制填写以便审阅时判断是否合理
     */
    String value() default "";
}
