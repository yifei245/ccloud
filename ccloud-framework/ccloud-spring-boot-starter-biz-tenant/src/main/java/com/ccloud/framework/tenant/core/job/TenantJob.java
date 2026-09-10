package com.ccloud.framework.tenant.core.job;

import java.lang.annotation.*;

/**
 * 标记定时任务需要按租户逐个执行。
 *
 * <p>不加该注解的任务运行在无租户上下文中，其查询会跨全部租户——
 * 对于"给每个租户发送日报"这类任务，必须加上它。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TenantJob {
}
