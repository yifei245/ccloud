package com.ccloud.module.infra.framework.tenant.core;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.module.system.api.tenant.TenantApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static com.ccloud.module.infra.enums.ErrorCodeConstants.ONLY_SYSTEM_TENANT;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class SystemTenantValidatorTest {

    private final TenantApi tenantApi = mock(TenantApi.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<TenantProperties> propertiesProvider = mock(ObjectProvider.class);
    private final SystemTenantValidator validator = new SystemTenantValidator(tenantApi, propertiesProvider);

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    @Test
    void 系统租户通过() {
        enableTenant(true);
        TenantContextHolder.setTenantId(1L);
        when(tenantApi.isSystemTenant(1L)).thenReturn(CommonResult.success(true));

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void 普通租户与不存在的租户被拒() {
        enableTenant(true);
        TenantContextHolder.setTenantId(2L);
        when(tenantApi.isSystemTenant(2L)).thenReturn(CommonResult.success(false));

        assertThatThrownBy(validator::validate).isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(ONLY_SYSTEM_TENANT.getCode());
    }

    @Test
    void 多租户关闭时视为系统租户_不调用RPC() {
        enableTenant(false);
        assertThatCode(validator::validate).doesNotThrowAnyException();

        when(propertiesProvider.getIfAvailable()).thenReturn(null);
        assertThatCode(validator::validate).doesNotThrowAnyException();

        verify(tenantApi, never()).isSystemTenant(anyLong());
    }

    private void enableTenant(boolean enable) {
        TenantProperties properties = new TenantProperties();
        properties.setEnable(enable);
        when(propertiesProvider.getIfAvailable()).thenReturn(properties);
    }
}
