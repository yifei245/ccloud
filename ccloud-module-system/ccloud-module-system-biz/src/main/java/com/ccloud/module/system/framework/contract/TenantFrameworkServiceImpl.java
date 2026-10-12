package com.ccloud.module.system.framework.contract;

import com.ccloud.framework.tenant.core.service.TenantFrameworkService;
import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import com.ccloud.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 租户校验（请求租户与登录租户一致、租户可用）与按租户遍历的定时任务所需的租户数据
 */
@Component
public class TenantFrameworkServiceImpl implements TenantFrameworkService {

    /**
     * 延迟注入：框架在初始化 MyBatis 拦截器时就要创建本类，而这些 Service 依赖 Mapper，
     * 直接注入会形成循环依赖。首次调用时才真正取得 Service
     */
    @Lazy
    @Resource
    private TenantService tenantService;

    @Override
    public List<Long> getTenantIds() {
        return tenantService.getTenantIdList();
    }

    @Override
    public void validTenant(Long id) {
        tenantService.validTenant(id);
    }

    @Override
    public boolean isSystemTenant(Long id) {
        TenantDO tenant = tenantService.getTenant(id);
        return tenant != null && tenant.isSystem();
    }
}
