package com.ccloud.module.system.service.tenant;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import com.ccloud.module.system.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;

/**
 * 租户 Service
 */
public interface TenantService {

    /**
     * 创建租户，并在该租户下创建租户管理员角色（菜单取自套餐）与管理员账号
     *
     * @return 租户编号
     */
    Long createTenant(@Valid TenantSaveReqVO createReqVO);

    /**
     * 修改租户；套餐变化时同步租户下各角色的菜单
     */
    void updateTenant(@Valid TenantSaveReqVO updateReqVO);

    /**
     * 按新的菜单范围刷新租户下全部角色：租户管理员与之完全一致，其他角色只收回超出范围的菜单
     */
    void updateTenantRoleMenu(Long tenantId, Set<Long> menuIds);

    void deleteTenant(Long id);

    void deleteTenantList(List<Long> ids);

    TenantDO getTenant(Long id);

    PageResult<TenantDO> getTenantPage(TenantPageReqVO pageReqVO);

    TenantDO getTenantByName(String name);

    TenantDO getTenantByWebsite(String website);

    Long getTenantCountByPackageId(Long packageId);

    List<TenantDO> getTenantListByPackageId(Long packageId);

    List<TenantDO> getTenantListByStatus(Integer status);

    /**
     * 以当前上下文租户执行；多租户关闭时不执行
     */
    void handleTenantInfo(TenantInfoHandler handler);

    /**
     * 以当前上下文租户的套餐菜单执行；多租户关闭或系统租户（不受套餐限制）时不执行
     */
    void handleTenantMenu(TenantMenuHandler handler);

    /**
     * @return 开启且未过期的租户编号，供按租户遍历的定时任务使用
     */
    List<Long> getTenantIdList();

    /**
     * 校验租户可用：不存在、已禁用、已过期分别抛出不同错误码
     */
    void validTenant(Long id);
}
