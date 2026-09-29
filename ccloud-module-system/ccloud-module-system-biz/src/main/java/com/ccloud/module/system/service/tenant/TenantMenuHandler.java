package com.ccloud.module.system.service.tenant;

import java.util.Set;

/**
 * 以当前租户可见的菜单范围执行逻辑，如过滤菜单树
 */
@FunctionalInterface
public interface TenantMenuHandler {

    /**
     * @param menuIds 当前租户套餐包含的菜单编号
     */
    void handle(Set<Long> menuIds);
}
