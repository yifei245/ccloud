package com.ccloud.module.member.framework.contract;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.api.tenant.TenantApi;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TenantFrameworkServiceImplTest {

    private final TenantApi tenantApi = mock(TenantApi.class);
    private final TenantFrameworkServiceImpl service = new TenantFrameworkServiceImpl(tenantApi, Duration.ofMinutes(1));

    @Test
    void validTenant_缓存期内同一租户只调用一次RPC() {
        when(tenantApi.validTenant(1L)).thenReturn(CommonResult.success(true));

        service.validTenant(1L);
        service.validTenant(1L);

        verify(tenantApi, times(1)).validTenant(1L);
    }

    @Test
    void validTenant_租户不可用时抛出system返回的错误码_且失败结果同样缓存() {
        when(tenantApi.validTenant(2L)).thenReturn(CommonResult.error(1_001_020_002, "名字为【x】的租户已过期"));

        assertThatThrownBy(() -> service.validTenant(2L)).isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(1_001_020_002);
        assertThatThrownBy(() -> service.validTenant(2L)).isInstanceOf(ServiceException.class);
        verify(tenantApi, times(1)).validTenant(2L);
    }

    @Test
    void validTenant_system不可用时不缓存_下次重试() {
        when(tenantApi.validTenant(3L)).thenThrow(new IllegalStateException("连接被拒绝"))
                .thenReturn(CommonResult.success(true));

        assertThatThrownBy(() -> service.validTenant(3L)).isInstanceOf(IllegalStateException.class);
        service.validTenant(3L);

        verify(tenantApi, times(2)).validTenant(3L);
    }

    @Test
    void getTenantIds_经RPC获取() {
        when(tenantApi.getTenantIdList()).thenReturn(CommonResult.success(List.of(1L, 2L)));
        assertThat(service.getTenantIds()).containsExactly(1L, 2L);
    }
}
