package com.ccloud.module.system.framework.contract;

import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionFrameworkService;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRespDTO;
import com.ccloud.module.system.service.permission.PermissionService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 部门数据权限规则的数据来源：按用户角色的数据范围计算可见部门
 */
@Component
public class DeptDataPermissionFrameworkServiceImpl implements DeptDataPermissionFrameworkService {

    /**
     * 延迟注入：框架在初始化 MyBatis 拦截器时就要创建本类，而这些 Service 依赖 Mapper，
     * 直接注入会形成循环依赖。首次调用时才真正取得 Service
     */
    @Lazy
    @Resource
    private PermissionService permissionService;

    @Override
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        return permissionService.getDeptDataPermission(userId);
    }
}
