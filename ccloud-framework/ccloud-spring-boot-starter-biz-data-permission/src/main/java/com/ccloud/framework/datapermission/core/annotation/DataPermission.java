package com.ccloud.framework.datapermission.core.annotation;

import java.lang.annotation.*;

/**
 * 开启数据权限。
 *
 * <p>采用<b>显式开启</b>而非默认全开：数据权限会改写 SQL，一旦无差别地作用于所有查询，
 * 内部统计、定时任务、关联查询都会被意外过滤，且失败是静默的。只有明确面向
 * "当前用户能看到哪些行"的查询才需要它。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataPermission {

    /**
     * 为 false 时在该方法内关闭数据权限，用于在已开启的类上豁免个别方法
     */
    boolean enable() default true;

    /**
     * 只生效这些规则；为空表示全部规则
     */
    Class<? extends com.ccloud.framework.datapermission.core.rule.DataPermissionRule>[] includeRules() default {};

    /**
     * 排除这些规则
     */
    Class<? extends com.ccloud.framework.datapermission.core.rule.DataPermissionRule>[] excludeRules() default {};
}
