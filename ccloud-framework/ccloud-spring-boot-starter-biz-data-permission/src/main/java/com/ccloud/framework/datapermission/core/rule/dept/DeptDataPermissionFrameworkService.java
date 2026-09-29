package com.ccloud.framework.datapermission.core.rule.dept;

/**
 * 部门数据权限查询契约。实现由 system 模块经 RPC 提供。
 */
public interface DeptDataPermissionFrameworkService {

    /**
     * 取用户的数据权限范围。用户有多个角色时，取其中<b>最宽</b>的那个。
     */
    DeptDataPermissionRespDTO getDeptDataPermission(Long userId);
}
