package com.ccloud.module.system.service.tenant;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.tenant.vo.packages.TenantPackagePageReqVO;
import com.ccloud.module.system.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantPackageDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 租户套餐 Service
 */
public interface TenantPackageService {

    Long createTenantPackage(@Valid TenantPackageSaveReqVO createReqVO);

    /**
     * 修改套餐；菜单范围变化时同步到使用该套餐的所有租户
     */
    void updateTenantPackage(@Valid TenantPackageSaveReqVO updateReqVO);

    /**
     * 删除套餐；仍有租户使用时拒绝
     */
    void deleteTenantPackage(Long id);

    void deleteTenantPackageList(List<Long> ids);

    TenantPackageDO getTenantPackage(Long id);

    PageResult<TenantPackageDO> getTenantPackagePage(TenantPackagePageReqVO pageReqVO);

    /**
     * 校验套餐存在且开启
     */
    TenantPackageDO validTenantPackage(Long id);

    List<TenantPackageDO> getTenantPackageListByStatus(Integer status);
}
