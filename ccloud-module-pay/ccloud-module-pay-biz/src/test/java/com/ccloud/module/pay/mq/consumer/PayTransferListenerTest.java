package com.ccloud.module.pay.mq.consumer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ccloud.framework.mq.outbox.core.IdempotentConsumer;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.test.core.ut.BaseMockitoUnitTest;
import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.pay.enums.transfer.PayTransferStatusEnum;
import com.ccloud.module.pay.mq.message.PayTransferMessage;
import com.ccloud.module.pay.service.notify.PayOpenNotifyService;
import com.ccloud.module.pay.support.IdempotentConsumerStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PayTransferListenerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private PayTransferSuccessListener successListener;
    @InjectMocks
    private PayTransferFailureListener failureListener;
    @Mock
    private IdempotentConsumer idempotentConsumer;
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

    private static PayTransferMessage message(PayTransferStatusEnum status) {
        return new PayTransferMessage().setTenantId(1L).setAppId(2L).setMerchantTransferId("MT1").setTransferNo("T1")
                .setPrice(300).setStatus(status.getStatus())
                .setSuccessTime(status == PayTransferStatusEnum.SUCCESS ? LocalDateTime.of(2026, 10, 12, 10, 0) : null)
                .setChannelErrorMsg(status == PayTransferStatusEnum.FAILURE ? "账户不存在" : null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> forwardedData(String messageId, String event, long appId) {
        ArgumentCaptor<Map<String, Object>> data = ArgumentCaptor.forClass(Map.class);
        verify(openNotifyService).forward(eq(messageId), eq(event), eq(appId), data.capture());
        return data.getValue();
    }

    @Test
    void 成功与失败监听器订阅各自的主题() {
        assertThat(successListener.getTopic()).isEqualTo("pay.transfer.success");
        assertThat(failureListener.getTopic()).isEqualTo("pay.transfer.failure");
        assertThat(successListener.getMessageType()).isEqualTo(PayTransferMessage.class);
        assertThat(failureListener.getMessageType()).isEqualTo(PayTransferMessage.class);
    }

    @Test
    void 转账成功只转发() {
        successListener.onMessage("msg-1", message(PayTransferStatusEnum.SUCCESS));

        Map<String, Object> data = forwardedData("msg-1", OpenNotifyEvents.PAY_TRANSFER_SUCCESS, 2L);
        assertThat(data).containsOnlyKeys("merchantTransferId", "transferNo", "price", "status", "successTime", "errorMsg");
        assertThat(data).containsEntry("merchantTransferId", "MT1").containsEntry("transferNo", "T1")
                .containsEntry("price", 300).containsEntry("status", "SUCCESS").containsEntry("errorMsg", null)
                .containsEntry("successTime", LocalDateTime.of(2026, 10, 12, 10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        assertThat(idempotency.calls).containsExactly("pay-open-notify");
    }

    @Test
    void 转账失败只转发_带失败原因() {
        failureListener.onMessage("msg-2", message(PayTransferStatusEnum.FAILURE));

        Map<String, Object> data = forwardedData("msg-2", OpenNotifyEvents.PAY_TRANSFER_FAILURE, 2L);
        assertThat(data).containsOnlyKeys("merchantTransferId", "transferNo", "price", "status", "successTime", "errorMsg");
        assertThat(data).containsEntry("status", "FAILURE").containsEntry("errorMsg", "账户不存在")
                .containsEntry("successTime", null);
        assertThat(idempotency.calls).containsExactly("pay-open-notify");
    }

    @Test
    void 消费线程切换到消息所属租户() {
        doAnswer(inv -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(5L);
            return null;
        }).when(openNotifyService).forward(any(), any(), anyLong(), any());
        successListener.onMessage("msg-3", message(PayTransferStatusEnum.SUCCESS).setTenantId(5L));
        verify(openNotifyService).forward(any(), any(), anyLong(), any());
    }

    /** 状态为空、非终态（等待、进行中）或未知：发送方有缺陷，抛异常让消息重投直至死信，不静默确认；不转发 */
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 5, 99})
    void 空或未知转账状态抛异常_不转发_记错误(Integer status) {
        assertRejected(() -> successListener.onMessage("msg-4", message(PayTransferStatusEnum.SUCCESS).setStatus(status)),
                "msg-4", "pay.transfer.success", String.valueOf(status));
        assertRejected(() -> failureListener.onMessage("msg-4", message(PayTransferStatusEnum.FAILURE).setStatus(status)),
                "msg-4", "pay.transfer.failure", String.valueOf(status));
    }

    @Test
    void 状态与主题不符时抛异常_成功消息到了失败主题_不转发() {
        assertRejected(() -> failureListener.onMessage("msg-6", message(PayTransferStatusEnum.SUCCESS)),
                "msg-6", "pay.transfer.failure", "10");
        assertRejected(() -> successListener.onMessage("msg-7", message(PayTransferStatusEnum.FAILURE)),
                "msg-7", "pay.transfer.success", "20");
    }

    @Test
    void 消息没有租户时抛异常_不转发() {
        assertThatThrownBy(() -> successListener.onMessage("msg-8", message(PayTransferStatusEnum.SUCCESS).setTenantId(null)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("msg-8").hasMessageContaining("租户");
        verifyNoInteractions(openNotifyService, idempotentConsumer);
    }

    private void assertRejected(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String messageId, String topic, String status) {
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractPayTransferListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(call).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(messageId).hasMessageContaining(topic).hasMessageContaining(status);
        } finally {
            logger.detachAppender(appender);
        }
        verifyNoInteractions(openNotifyService, idempotentConsumer);
        assertThat(appender.list).anyMatch(e -> e.getLevel() == Level.ERROR && e.getFormattedMessage().contains(messageId)
                && e.getFormattedMessage().contains(topic));
    }

    @Test
    void 转发失败时抛出_消息重投时再次转发() {
        doThrow(new IllegalStateException("开放平台不可用")).doAnswer(inv -> null)
                .when(openNotifyService).forward(any(), any(), anyLong(), any());
        PayTransferMessage message = message(PayTransferStatusEnum.SUCCESS);

        assertThatThrownBy(() -> successListener.onMessage("msg-5", message)).isInstanceOf(IllegalStateException.class);
        assertThat(idempotency.consumed).isEmpty();

        successListener.onMessage("msg-5", message);
        assertThat(idempotency.consumed).containsExactly("msg-5|pay-open-notify");
    }
}
