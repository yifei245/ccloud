package com.ccloud.framework.tenant.core.service;

import com.ccloud.framework.common.exception.ErrorCode;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SystemTenantValidatorTest {

    private static final ErrorCode ONLY = new ErrorCode(1_099_000_000, "仅限系统租户");

    private final TenantFrameworkService tenantFrameworkService = mock(TenantFrameworkService.class);

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    private SystemTenantValidator validator(Boolean enable) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        if (enable != null) {
            TenantProperties properties = new TenantProperties();
            properties.setEnable(enable);
            factory.addBean("tenantProperties", properties);
        }
        factory.addBean("tenantFrameworkService", tenantFrameworkService);
        return new SystemTenantValidator(factory.getBeanProvider(TenantFrameworkService.class),
                factory.getBeanProvider(TenantProperties.class));
    }

    @Test
    void 系统租户通过_普通租户抛出指定错误码() {
        when(tenantFrameworkService.isSystemTenant(1L)).thenReturn(true);
        when(tenantFrameworkService.isSystemTenant(2L)).thenReturn(false);
        TenantContextHolder.setTenantId(1L);
        assertThatNoException().isThrownBy(() -> validator(true).validate(ONLY));
        TenantContextHolder.setTenantId(2L);
        assertThatThrownBy(() -> validator(true).validate(ONLY))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(ONLY.getCode());
    }

    @Test
    void 多租户关闭或未配置时不校验() {
        assertThatNoException().isThrownBy(() -> validator(false).validate(ONLY));
        assertThatNoException().isThrownBy(() -> validator(null).validate(ONLY));
        verifyNoInteractions(tenantFrameworkService);
    }
}
