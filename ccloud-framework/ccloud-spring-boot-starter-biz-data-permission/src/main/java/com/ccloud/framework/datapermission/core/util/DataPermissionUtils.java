package com.ccloud.framework.datapermission.core.util;

import com.ccloud.framework.datapermission.core.annotation.DataPermission;
import com.ccloud.framework.datapermission.core.aop.DataPermissionContextHolder;

import java.util.function.Supplier;

/**
 * 数据权限开关工具
 */
public class DataPermissionUtils {

    /**
     * 关闭数据权限的注解实例。用匿名实现而非反射构造，避免每次创建开销。
     */
    private static final DataPermission DATA_PERMISSION_DISABLE = new DataPermission() {

        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return DataPermission.class;
        }

        @Override
        public boolean enable() {
            return false;
        }

        @Override
        public Class<? extends com.ccloud.framework.datapermission.core.rule.DataPermissionRule>[] includeRules() {
            return new Class[0];
        }

        @Override
        public Class<? extends com.ccloud.framework.datapermission.core.rule.DataPermissionRule>[] excludeRules() {
            return new Class[0];
        }
    };

    /**
     * 在忽略数据权限的上下文中执行。用于系统级操作、跨部门统计等。
     */
    public static void executeIgnore(Runnable runnable) {
        DataPermissionContextHolder.add(DATA_PERMISSION_DISABLE);
        try {
            runnable.run();
        } finally {
            DataPermissionContextHolder.remove();
        }
    }

    public static <T> T executeIgnore(Supplier<T> supplier) {
        DataPermissionContextHolder.add(DATA_PERMISSION_DISABLE);
        try {
            return supplier.get();
        } finally {
            DataPermissionContextHolder.remove();
        }
    }
}
