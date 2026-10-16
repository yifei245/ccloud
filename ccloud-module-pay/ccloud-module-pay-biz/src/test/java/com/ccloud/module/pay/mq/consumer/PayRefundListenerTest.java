package com.ccloud.module.pay.mq.consumer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ccloud.framework.mq.outbox.core.IdempotentConsumer;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.test.core.ut.BaseMockitoUnitTest;
import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.mq.message.PayRefundMessage;
import com.ccloud.module.pay.service.notify.PayOpenNotifyService;
import com.ccloud.module.pay.service.wallet.PayWalletRechargeService;
import com.ccloud.module.pay.support.IdempotentConsumerStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PayRefundListenerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private PayRefundSuccessListener successListener;
    @InjectMocks
    private PayRefundFailureListener failureListener;
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

    private static PayRefundMessage message(PayRefundStatusEnum status) {
        return new PayRefundMessage().setTenantId(1L).setAppId(2L).setMerchantOrderId("WR1").setMerchantRefundId("WR1-R")
                .setRefundNo("R1").setRefundPrice(100).setStatus(status.getStatus())
                .setSuccessTime(status == PayRefundStatusEnum.SUCCESS ? LocalDateTime.of(2026, 10, 12, 10, 0) : null)
                .setChannelErrorMsg(status == PayRefundStatusEnum.FAILURE ? "渠道拒绝" : null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> forwardedData(String messageId, String event, long appId) {
        ArgumentCaptor<Map<String, Object>> data = ArgumentCaptor.forClass(Map.class);
        verify(openNotifyService).forward(eq(messageId), eq(event), eq(appId), data.capture());
        return data.getValue();
    }

    @Test
    void 成功与失败监听器订阅各自的主题() {
        assertThat(successListener.getTopic()).isEqualTo("pay.refund.success");
        assertThat(failureListener.getTopic()).isEqualTo("pay.refund.failure");
        assertThat(successListener.getMessageType()).isEqualTo(PayRefundMessage.class);
        assertThat(failureListener.getMessageType()).isEqualTo(PayRefundMessage.class);
    }

    @Test
    void 充值应用的退款成功先处理充值再转发() {
        when(rechargeService.isRechargeApp(2L)).thenReturn(true);
        PayRefundMessage message = message(PayRefundStatusEnum.SUCCESS);
        successListener.onMessage("msg-1", message);

        InOrder order = inOrder(idempotentConsumer);
        order.verify(idempotentConsumer).consumeOnce(eq("msg-1"), eq("pay-wallet-recharge"), any());
        order.verify(idempotentConsumer).consumeOnce(eq("msg-1"), eq("pay-open-notify"), any());
        verify(rechargeService).handleRechargeRefunded(message);
        Map<String, Object> data = forwardedData("msg-1", OpenNotifyEvents.PAY_REFUND_SUCCESS, 2L);
        assertThat(data).containsOnlyKeys("merchantOrderId", "merchantRefundId", "refundNo", "refundPrice", "status",
                "successTime", "errorMsg");
        assertThat(data).containsEntry("merchantOrderId", "WR1").containsEntry("merchantRefundId", "WR1-R")
                .containsEntry("refundNo", "R1").containsEntry("refundPrice", 100).containsEntry("status", "SUCCESS")
                .containsEntry("successTime", LocalDateTime.of(2026, 10, 12, 10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .containsEntry("errorMsg", null);
    }

    @Test
    void 充值应用的退款失败先处理充值再转发_带失败原因() {
        when(rechargeService.isRechargeApp(2L)).thenReturn(true);
        PayRefundMessage message = message(PayRefundStatusEnum.FAILURE);
        failureListener.onMessage("msg-2", message);

        InOrder order = inOrder(idempotentConsumer);
        order.verify(idempotentConsumer).consumeOnce(eq("msg-2"), eq("pay-wallet-recharge"), any());
        order.verify(idempotentConsumer).consumeOnce(eq("msg-2"), eq("pay-open-notify"), any());
        verify(rechargeService).handleRechargeRefunded(message);
        Map<String, Object> data = forwardedData("msg-2", OpenNotifyEvents.PAY_REFUND_FAILURE, 2L);
        assertThat(data).containsOnlyKeys("merchantOrderId", "merchantRefundId", "refundNo", "refundPrice", "status",
                "successTime", "errorMsg");
        assertThat(data).containsEntry("status", "FAILURE").containsEntry("errorMsg", "渠道拒绝")
                .containsEntry("successTime", null);
    }

    @Test
    void 非充值应用只转发() {
        when(rechargeService.isRechargeApp(2L)).thenReturn(false);
        successListener.onMessage("msg-3", message(PayRefundStatusEnum.SUCCESS));
        verify(rechargeService, never()).handleRechargeRefunded(any());
        verify(openNotifyService).forward(eq("msg-3"), eq(OpenNotifyEvents.PAY_REFUND_SUCCESS), eq(2L), any());
        assertThat(idempotency.calls).containsExactly("pay-open-notify");
    }

    @Test
    void 消费线程切换到消息所属租户() {
        when(rechargeService.isRechargeApp(anyLong())).thenAnswer(inv -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(5L);
            return true;
        });
        doAnswer(inv -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(5L);
            return null;
        }).when(rechargeService).handleRechargeRefunded(any());
        doAnswer(inv -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(5L);
            return null;
        }).when(openNotifyService).forward(any(), any(), anyLong(), any());
        failureListener.onMessage("msg-4", message(PayRefundStatusEnum.FAILURE).setTenantId(5L));
        verify(rechargeService).handleRechargeRefunded(any());
        verify(openNotifyService).forward(any(), any(), anyLong(), any());
    }

    /** 状态为空、非终态或未知：发送方有缺陷，抛异常让消息重投直至死信，不静默确认；两件事都不执行 */
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 99})
    void 空或未知退款状态抛异常_不处理也不转发_记错误(Integer status) {
        assertRejected(() -> successListener.onMessage("msg-5", message(PayRefundStatusEnum.SUCCESS).setStatus(status)),
                "msg-5", "pay.refund.success", String.valueOf(status));
        assertRejected(() -> failureListener.onMessage("msg-5", message(PayRefundStatusEnum.FAILURE).setStatus(status)),
                "msg-5", "pay.refund.failure", String.valueOf(status));
    }

    @Test
    void 状态与主题不符时抛异常_成功消息到了失败主题_不处理也不转发() {
        assertRejected(() -> failureListener.onMessage("msg-7", message(PayRefundStatusEnum.SUCCESS)),
                "msg-7", "pay.refund.failure", "10");
        assertRejected(() -> successListener.onMessage("msg-8", message(PayRefundStatusEnum.FAILURE)),
                "msg-8", "pay.refund.success", "20");
    }

    @Test
    void 消息没有租户时抛异常_任何步骤都不执行() {
        assertThatThrownBy(() -> successListener.onMessage("msg-9", message(PayRefundStatusEnum.SUCCESS).setTenantId(null)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("msg-9").hasMessageContaining("租户");
        verifyNoInteractions(rechargeService, openNotifyService, idempotentConsumer);
    }

    private void assertRejected(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String messageId, String topic, String status) {
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractPayRefundListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(call).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(messageId).hasMessageContaining(topic).hasMessageContaining(status);
        } finally {
            logger.detachAppender(appender);
        }
        verifyNoInteractions(rechargeService, openNotifyService, idempotentConsumer);
        assertThat(appender.list).anyMatch(e -> e.getLevel() == Level.ERROR && e.getFormattedMessage().contains(messageId)
                && e.getFormattedMessage().contains(topic));
    }

    @Test
    void 入账成功转发失败后重投_跳过充值处理只重试转发() {
        when(rechargeService.isRechargeApp(2L)).thenReturn(true);
        doThrow(new IllegalStateException("开放平台不可用")).doAnswer(inv -> null)
                .when(openNotifyService).forward(any(), any(), anyLong(), any());
        PayRefundMessage message = message(PayRefundStatusEnum.FAILURE);

        assertThatThrownBy(() -> failureListener.onMessage("msg-6", message)).isInstanceOf(IllegalStateException.class);
        assertThat(idempotency.consumed).containsExactly("msg-6|pay-wallet-recharge");

        failureListener.onMessage("msg-6", message);

        verify(rechargeService, times(1)).handleRechargeRefunded(message);
        verify(openNotifyService, times(2)).forward(eq("msg-6"), eq(OpenNotifyEvents.PAY_REFUND_FAILURE), eq(2L), any());
        assertThat(idempotency.consumed).containsExactlyInAnyOrder("msg-6|pay-wallet-recharge", "msg-6|pay-open-notify");
    }
}
