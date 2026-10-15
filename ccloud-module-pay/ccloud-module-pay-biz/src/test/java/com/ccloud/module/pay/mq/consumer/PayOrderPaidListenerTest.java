package com.ccloud.module.pay.mq.consumer;

import com.ccloud.framework.mq.outbox.core.IdempotentConsumer;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.test.core.ut.BaseMockitoUnitTest;
import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.pay.mq.message.PayOrderMessage;
import com.ccloud.module.pay.service.notify.PayOpenNotifyService;
import com.ccloud.module.pay.service.wallet.PayWalletRechargeService;
import com.ccloud.module.pay.support.IdempotentConsumerStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayOrderPaidListenerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private PayOrderPaidListener listener;
    @Mock
    private IdempotentConsumer idempotentConsumer;
    @Mock
    private PayWalletRechargeService rechargeService;
    @Mock
    private PayOpenNotifyService openNotifyService;

    private IdempotentConsumerStub idempotency;

    @BeforeEach
    void installIdempotency() {
        idempotency = IdempotentConsumerStub.install(idempotentConsumer);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> forwardedData(String messageId, long appId) {
        ArgumentCaptor<Map<String, Object>> data = ArgumentCaptor.forClass(Map.class);
        verify(openNotifyService).forward(eq(messageId), eq(OpenNotifyEvents.PAY_ORDER_PAID), eq(appId), data.capture());
        return data.getValue();
    }

    @Test
    void 订阅支付成功主题() {
        assertThat(listener.getTopic()).isEqualTo("pay.order.paid");
        assertThat(listener.getMessageType()).isEqualTo(PayOrderMessage.class);
    }

    @Test
    void 充值应用的订单先入账再转发_两个消费者分别幂等() {
        when(rechargeService.isRechargeApp(2L)).thenReturn(true);
        LocalDateTime successTime = LocalDateTime.of(2026, 10, 12, 10, 0);
        PayOrderMessage message = new PayOrderMessage().setTenantId(1L).setAppId(2L).setMerchantOrderId("WR1")
                .setOrderNo("P1").setPrice(100).setChannelCode("mock").setSuccessTime(successTime);
        listener.onMessage("msg-1", message);

        InOrder order = inOrder(idempotentConsumer);
        order.verify(idempotentConsumer).consumeOnce(eq("msg-1"), eq("pay-wallet-recharge"), any());
        order.verify(idempotentConsumer).consumeOnce(eq("msg-1"), eq("pay-open-notify"), any());
        verify(rechargeService).handleRechargePaid(message);
        Map<String, Object> data = forwardedData("msg-1", 2L);
        assertThat(data).containsOnlyKeys("merchantOrderId", "orderNo", "price", "status", "successTime", "channelCode");
        assertThat(data).containsEntry("merchantOrderId", "WR1").containsEntry("orderNo", "P1")
                .containsEntry("price", 100).containsEntry("status", "SUCCESS").containsEntry("channelCode", "mock")
                .containsEntry("successTime", successTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    @Test
    void 成功时间为空时回调数据中为null() {
        listener.onMessage("msg-n", new PayOrderMessage().setTenantId(1L).setAppId(3L).setPrice(1));
        assertThat(forwardedData("msg-n", 3L)).containsKey("successTime").containsEntry("successTime", null);
    }

    @Test
    void 非充值应用只转发() {
        when(rechargeService.isRechargeApp(3L)).thenReturn(false);
        listener.onMessage("msg-2", new PayOrderMessage().setTenantId(1L).setAppId(3L).setPrice(1));
        verify(rechargeService, never()).handleRechargePaid(any());
        verify(openNotifyService).forward(eq("msg-2"), eq(OpenNotifyEvents.PAY_ORDER_PAID), eq(3L), any());
        assertThat(idempotency.calls).containsExactly("pay-open-notify");
    }

    @Test
    void 消费线程切换到消息所属租户() {
        when(rechargeService.isRechargeApp(anyLong())).thenAnswer(inv -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(5L);
            return false;
        });
        doNothing().when(openNotifyService).forward(any(), any(), anyLong(), any());
        listener.onMessage("msg-3", new PayOrderMessage().setTenantId(5L).setAppId(3L).setPrice(1));
        verify(rechargeService).isRechargeApp(3L);
    }

    @Test
    void 转发时也处于消息所属租户() {
        doAnswer(inv -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(5L);
            return null;
        }).when(openNotifyService).forward(any(), any(), anyLong(), any());
        listener.onMessage("msg-4", new PayOrderMessage().setTenantId(5L).setAppId(3L).setPrice(1));
        verify(openNotifyService).forward(eq("msg-4"), any(), eq(3L), any());
    }

    /** 入账成功而转发抛异常：消息不 ACK 被重投，重投时入账已消费应跳过，只重试转发 */
    @Test
    void 入账成功转发失败后重投_跳过入账只重试转发() {
        when(rechargeService.isRechargeApp(2L)).thenReturn(true);
        doThrow(new IllegalStateException("开放平台不可用")).doNothing()
                .when(openNotifyService).forward(any(), any(), anyLong(), any());
        PayOrderMessage message = new PayOrderMessage().setTenantId(1L).setAppId(2L).setMerchantOrderId("WR1").setPrice(100);

        assertThatThrownBy(() -> listener.onMessage("msg-5", message)).isInstanceOf(IllegalStateException.class);
        assertThat(idempotency.consumed).containsExactly("msg-5|pay-wallet-recharge");

        listener.onMessage("msg-5", message);

        verify(rechargeService, times(1)).handleRechargePaid(message);
        verify(openNotifyService, times(2)).forward(eq("msg-5"), eq(OpenNotifyEvents.PAY_ORDER_PAID), eq(2L), any());
        assertThat(idempotency.consumed).containsExactlyInAnyOrder("msg-5|pay-wallet-recharge", "msg-5|pay-open-notify");
        assertThat(idempotency.calls).containsExactly("pay-wallet-recharge", "pay-open-notify",
                "pay-wallet-recharge", "pay-open-notify");
    }

    @Test
    void 入账失败时不转发_消息重投() {
        when(rechargeService.isRechargeApp(2L)).thenReturn(true);
        doThrow(new IllegalStateException("db down")).when(rechargeService).handleRechargePaid(any());
        assertThatThrownBy(() -> listener.onMessage("msg-6", new PayOrderMessage().setTenantId(1L).setAppId(2L)))
                .isInstanceOf(IllegalStateException.class);
        verify(openNotifyService, never()).forward(any(), any(), anyLong(), any());
        assertThat(idempotency.consumed).isEmpty();
    }
}
