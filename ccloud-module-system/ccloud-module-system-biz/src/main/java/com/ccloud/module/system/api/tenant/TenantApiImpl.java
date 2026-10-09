package com.ccloud.module.system.api.tenant;

import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import com.ccloud.module.system.service.tenant.TenantService;
import com.ccloud.framework.common.pojo.CommonResult;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 租户 RPC 实现：各服务的租户校验与按租户遍历的定时任务依赖它
 */
@RestController
@Validated
public class TenantApiImpl implements TenantApi {

    @Resource
    private TenantService tenantService;

    @Override
    public CommonResult<List<Long>> getTenantIdList() {
        return success(tenantService.getTenantIdList());
    }

    @Override
    public CommonResult<Boolean> validTenant(Long id) {
        tenantService.validTenant(id);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> isSystemTenant(Long id) {
        TenantDO tenant = tenantService.getTenant(id);
        return success(tenant != null && tenant.isSystem());
    }
}
