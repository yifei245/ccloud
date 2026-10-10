package com.ccloud.framework.tenant.core.service;

import com.ccloud.framework.common.exception.ErrorCode;
import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import org.springframework.beans.factory.ObjectProvider;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 全局数据（不分租户的表）或跨租户数据的维护只允许系统租户执行。套餐只控制菜单是否可见，
 * 平台管理员误把这些菜单放进普通套餐时，这里是最后一道防线。
 *
 * <p>与 system 的 {@code MenuServiceImpl.validateSystemTenant()} 同一规则；多租户关闭时视为系统租户
 */
public class SystemTenantValidator {

    private final ObjectProvider<TenantFrameworkService> tenantFrameworkServiceProvider;
    private final ObjectProvider<TenantProperties> tenantPropertiesProvider;

    public SystemTenantValidator(ObjectProvider<TenantFrameworkService> tenantFrameworkServiceProvider,
                                 ObjectProvider<TenantProperties> tenantPropertiesProvider) {
        this.tenantFrameworkServiceProvider = tenantFrameworkServiceProvider;
        this.tenantPropertiesProvider = tenantPropertiesProvider;
    }

    /**
     * @param errorCode 当前租户不是系统租户时抛出的错误码，由各模块提供（文案需各自登记国际化）
     */
    public void validate(ErrorCode errorCode) {
        TenantProperties properties = tenantPropertiesProvider.getIfAvailable();
        if (properties == null || !Boolean.TRUE.equals(properties.getEnable())) {
            return;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!tenantFrameworkServiceProvider.getObject().isSystemTenant(tenantId)) {
            throw exception(errorCode);
        }
    }
}
