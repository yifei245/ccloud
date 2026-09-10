package com.ccloud.framework.tenant.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Objects;

/**
 * 租户上下文。
 *
 * <p>用 {@link TransmittableThreadLocal} 而非 {@code InheritableThreadLocal}：
 * 后者只在创建线程时传递一次，线程池复用已有线程时拿到的是上一个任务残留的值——
 * 在多租户场景下这意味着任务可能带着别的租户身份执行。
 */
public class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new TransmittableThreadLocal<>();

    /**
     * 是否忽略租户隔离。仅供系统级操作（跨租户统计、初始化、定时任务遍历）使用。
     */
    private static final ThreadLocal<Boolean> IGNORE = new TransmittableThreadLocal<>();

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * @throws NullPointerException 上下文中没有租户时抛出。缺租户是配置或链路错误，
     *                              应当立刻暴露而不是静默按 null 处理
     */
    public static Long getRequiredTenantId() {
        Long tenantId = getTenantId();
        return Objects.requireNonNull(tenantId,
                "当前线程缺少租户上下文，请检查请求是否携带 X-Tenant-Id，或是否遗漏 TenantUtils.execute 包装");
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static void setIgnore(Boolean ignore) {
        IGNORE.set(ignore);
    }

    public static boolean isIgnore() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    public static void clear() {
        TENANT_ID.remove();
        IGNORE.remove();
    }
}
