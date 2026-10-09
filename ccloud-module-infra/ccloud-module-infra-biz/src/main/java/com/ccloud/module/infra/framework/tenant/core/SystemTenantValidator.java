package com.ccloud.module.infra.framework.tenant.core;

import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.module.system.api.tenant.TenantApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.ONLY_SYSTEM_TENANT;

/**
 * 参数配置、文件配置不按租户隔离，消息死信的管理天然跨租户：维护它们的操作只允许系统租户（套餐 0）执行。
 * 套餐只控制菜单是否可见，平台管理员误把这些菜单放进普通套餐时，这里是最后一道防线。
 *
 * <p>与 system 的 {@code MenuServiceImpl.validateSystemTenant()} 同一规则；多租户关闭时视为系统租户
 */
@Component
public class SystemTenantValidator {

    private final TenantApi tenantApi;
    private final ObjectProvider<TenantProperties> tenantPropertiesProvider;

    public SystemTenantValidator(@Lazy TenantApi tenantApi, ObjectProvider<TenantProperties> tenantPropertiesProvider) {
        this.tenantApi = tenantApi;
        this.tenantPropertiesProvider = tenantPropertiesProvider;
    }

    public void validate() {
        if (isTenantDisable()) {
            return;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!Boolean.TRUE.equals(tenantApi.isSystemTenant(tenantId).getCheckedData())) {
            throw exception(ONLY_SYSTEM_TENANT);
        }
    }

    private boolean isTenantDisable() {
        TenantProperties properties = tenantPropertiesProvider.getIfAvailable();
        return properties == null || !Boolean.TRUE.equals(properties.getEnable());
    }
}
