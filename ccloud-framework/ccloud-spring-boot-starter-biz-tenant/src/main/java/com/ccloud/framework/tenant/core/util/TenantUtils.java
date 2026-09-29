package com.ccloud.framework.tenant.core.util;

import com.ccloud.framework.tenant.core.context.TenantContextHolder;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 租户上下文切换工具。
 *
 * <p>所有方法都会在结束后<b>恢复</b>原有上下文而非清空——嵌套调用很常见，
 * 简单清空会让外层失去租户身份，后续查询悄悄变成全租户可见。
 */
public class TenantUtils {

    public static void execute(Long tenantId, Runnable runnable) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        boolean oldIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.setIgnore(false);
        try {
            runnable.run();
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    public static <V> V execute(Long tenantId, Supplier<V> supplier) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        boolean oldIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.setIgnore(false);
        try {
            return supplier.get();
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    public static void executeIgnore(Runnable runnable) {
        boolean oldIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setIgnore(true);
        try {
            runnable.run();
        } finally {
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    public static <V> V executeIgnore(Supplier<V> supplier) {
        boolean oldIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setIgnore(true);
        try {
            return supplier.get();
        } finally {
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    public static <V> V executeIgnoreCallable(Callable<V> callable) throws Exception {
        boolean oldIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setIgnore(true);
        try {
            return callable.call();
        } finally {
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }
}
