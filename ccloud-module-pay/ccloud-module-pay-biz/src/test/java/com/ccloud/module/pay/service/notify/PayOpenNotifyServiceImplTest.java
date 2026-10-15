package com.ccloud.module.pay.service.notify;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.test.core.ut.BaseMockitoUnitTest;
import com.ccloud.module.open.api.notify.OpenNotifyApi;
import com.ccloud.module.open.api.notify.dto.OpenNotifySendReqDTO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.service.app.PayAppService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PayOpenNotifyServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private PayOpenNotifyServiceImpl service;
    @Mock
    private PayAppService appService;
    @Mock
    private OpenNotifyApi openNotifyApi;

    @Test
    void 重投时以消息编号作为事件编号_发给绑定的开放应用() {
        when(appService.getAppIgnoreTenant(1L)).thenReturn(new PayAppDO().setId(1L).setOpenAppId(9L));
        when(openNotifyApi.sendNotify(any())).thenReturn(CommonResult.success(100L));
        service.forward("msg-001", "pay.order.paid", 1L, Map.of("merchantOrderId", "M1"));

        ArgumentCaptor<OpenNotifySendReqDTO> captor = ArgumentCaptor.forClass(OpenNotifySendReqDTO.class);
        verify(openNotifyApi).sendNotify(captor.capture());
        assertThat(captor.getValue().getAppId()).isEqualTo(9L);
        assertThat(captor.getValue().getEventId()).isEqualTo("msg-001");
        assertThat(captor.getValue().getEvent()).isEqualTo("pay.order.paid");
        assertThat(captor.getValue().getData()).containsExactly(Map.entry("merchantOrderId", "M1"));
    }

    @Test
    void 未绑定开放应用时不发送() {
        when(appService.getAppIgnoreTenant(1L)).thenReturn(new PayAppDO().setId(1L));
        service.forward("msg-002", "pay.order.paid", 1L, Map.of());
        verifyNoInteractions(openNotifyApi);
    }

    @Test
    void 支付应用不存在时静默返回() {
        when(appService.getAppIgnoreTenant(1L)).thenReturn(null);
        service.forward("msg-004", "pay.order.paid", 1L, Map.of());
        verifyNoInteractions(openNotifyApi);
    }

    @Test
    void 开放平台返回失败时抛出_让消息重投() {
        when(appService.getAppIgnoreTenant(1L)).thenReturn(new PayAppDO().setId(1L).setOpenAppId(9L));
        when(openNotifyApi.sendNotify(any())).thenReturn(CommonResult.error(500, "down"));
        assertThatThrownBy(() -> service.forward("msg-003", "pay.order.paid", 1L, Map.of())).isInstanceOf(ServiceException.class);
    }

    @Test
    void 远程调用异常时原样抛出_让消息重投() {
        when(appService.getAppIgnoreTenant(1L)).thenReturn(new PayAppDO().setId(1L).setOpenAppId(9L));
        IllegalStateException timeout = new IllegalStateException("read timed out");
        when(openNotifyApi.sendNotify(any())).thenThrow(timeout);
        assertThatThrownBy(() -> service.forward("msg-005", "pay.order.paid", 1L, Map.of())).isSameAs(timeout);
    }
}
