package com.ccloud.framework.tenant.config;

import com.ccloud.framework.tenant.core.service.SystemTenantValidator;
import com.ccloud.framework.tenant.core.service.TenantFrameworkService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 系统租户校验。与多租户开关无关、无条件注册：业务服务直接注入 {@link SystemTenantValidator}，
 * 多租户关闭时它不做校验
 */
@AutoConfiguration
public class CcloudSystemTenantValidatorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SystemTenantValidator systemTenantValidator(ObjectProvider<TenantFrameworkService> tenantFrameworkService,
                                                       ObjectProvider<TenantProperties> tenantProperties) {
        return new SystemTenantValidator(tenantFrameworkService, tenantProperties);
    }
}
