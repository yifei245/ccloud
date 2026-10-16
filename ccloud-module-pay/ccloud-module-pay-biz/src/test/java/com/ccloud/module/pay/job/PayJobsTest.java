package com.ccloud.module.pay.job;

import com.ccloud.framework.mq.redis.core.RedisStreamPendingMessageResendJob;
import com.ccloud.framework.test.core.ut.BaseMockitoUnitTest;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.job.mq.MqJobHandler;
import com.ccloud.module.pay.job.order.PayOrderExpireJob;
import com.ccloud.module.pay.job.order.PayOrderSyncJob;
import com.ccloud.module.pay.job.refund.PayRefundSyncJob;
import com.ccloud.module.pay.job.transfer.PayTransferSyncJob;
import com.ccloud.module.pay.service.order.PayOrderService;
import com.ccloud.module.pay.service.refund.PayRefundService;
import com.ccloud.module.pay.service.transfer.PayTransferService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayJobsTest extends BaseMockitoUnitTest {

    @Mock
    private PayOrderService orderService;
    @Mock
    private PayRefundService refundService;
    @Mock
    private PayTransferService transferService;
    @Spy
    private PayProperties payProperties = new PayProperties();

    @InjectMocks
    private PayOrderExpireJob expireJob;
    @InjectMocks
    private PayOrderSyncJob orderSyncJob;
    @InjectMocks
    private PayRefundSyncJob refundSyncJob;
    @InjectMocks
    private PayTransferSyncJob transferSyncJob;

    @Test
    void 各任务按配置的批量调用对应服务并上报处理条数() {
        payProperties.getJob().setBatchSize(37);
        when(orderService.expireOrder(37)).thenReturn(1);
        when(orderService.syncOrder(37)).thenReturn(2);
        when(refundService.syncRefund(37)).thenReturn(3);
        when(transferService.syncTransfer(37)).thenReturn(4);

        try (MockedStatic<XxlJobHelper> helper = mockStatic(XxlJobHelper.class)) {
            expireJob.execute();
            orderSyncJob.execute();
            refundSyncJob.execute();
            transferSyncJob.execute();

            helper.verify(() -> XxlJobHelper.handleSuccess("关闭超时订单 1 笔"));
            helper.verify(() -> XxlJobHelper.handleSuccess("补查等待中的支付尝试 2 笔"));
            helper.verify(() -> XxlJobHelper.handleSuccess("补查等待中的退款 3 笔"));
            helper.verify(() -> XxlJobHelper.handleSuccess("补查等待中的转账 4 笔"));
        }
        verify(orderService).expireOrder(37);
        verify(orderService).syncOrder(37);
        verify(refundService).syncRefund(37);
        verify(transferService).syncTransfer(37);
    }

    @Test
    void 服务抛异常时任务原样抛出且不上报成功() {
        RuntimeException boom = new RuntimeException("boom");
        when(orderService.expireOrder(100)).thenThrow(boom);
        when(orderService.syncOrder(100)).thenThrow(boom);
        when(refundService.syncRefund(100)).thenThrow(boom);
        when(transferService.syncTransfer(100)).thenThrow(boom);

        try (MockedStatic<XxlJobHelper> helper = mockStatic(XxlJobHelper.class)) {
            assertSame(boom, assertThrows(RuntimeException.class, expireJob::execute));
            assertSame(boom, assertThrows(RuntimeException.class, orderSyncJob::execute));
            assertSame(boom, assertThrows(RuntimeException.class, refundSyncJob::execute));
            assertSame(boom, assertThrows(RuntimeException.class, transferSyncJob::execute));

            helper.verifyNoInteractions();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void 消息重投任务委托框架任务并上报其结果() {
        ObjectProvider<RedisStreamPendingMessageResendJob> provider = mock(ObjectProvider.class);
        RedisStreamPendingMessageResendJob resendJob = mock(RedisStreamPendingMessageResendJob.class);
        when(provider.getIfAvailable()).thenReturn(resendJob);
        when(resendJob.execute()).thenReturn("重投 2 条");

        try (MockedStatic<XxlJobHelper> helper = mockStatic(XxlJobHelper.class)) {
            new MqJobHandler(provider).redisStreamPendingMessageResend();

            helper.verify(() -> XxlJobHelper.handleSuccess("重投 2 条"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void 没有流监听器时消息重投任务直接上报成功() {
        ObjectProvider<RedisStreamPendingMessageResendJob> provider = mock(ObjectProvider.class);

        try (MockedStatic<XxlJobHelper> helper = mockStatic(XxlJobHelper.class)) {
            new MqJobHandler(provider).redisStreamPendingMessageResend();

            helper.verify(() -> XxlJobHelper.handleSuccess("无 Redis Stream 监听器"));
        }
    }

    @Test
    void 各处理器带有约定名称的XxlJob注解且名称在pay内唯一() {
        List<Class<?>> classes = List.of(PayOrderExpireJob.class, PayOrderSyncJob.class,
                PayRefundSyncJob.class, PayTransferSyncJob.class, MqJobHandler.class);
        List<String> names = classes.stream().map(PayJobsTest::xxlJobNames).flatMap(List::stream).toList();

        assertEquals(List.of("payOrderExpireJob", "payOrderSyncJob", "payRefundSyncJob", "payTransferSyncJob",
                "redisStreamPendingMessageResendJob"), names);
        Set<String> unique = names.stream().collect(Collectors.toSet());
        assertEquals(names.size(), unique.size());
        classes.forEach(clazz -> assertNotNull(clazz.getAnnotation(Component.class), clazz.getSimpleName()));
    }

    @Test
    void 任务不带租户任务注解() {
        List<Class<?>> classes = List.of(PayOrderExpireJob.class, PayOrderSyncJob.class,
                PayRefundSyncJob.class, PayTransferSyncJob.class, MqJobHandler.class);
        for (Class<?> clazz : classes) {
            assertFalse(Arrays.stream(clazz.getDeclaredMethods()).flatMap(m -> Arrays.stream(m.getAnnotations()))
                    .anyMatch(a -> a.annotationType().getSimpleName().equals("TenantJob")), clazz.getSimpleName());
            assertTrue(Arrays.stream(clazz.getDeclaredMethods())
                    .anyMatch(m -> m.isAnnotationPresent(XxlJob.class)), clazz.getSimpleName());
        }
    }

    private static List<String> xxlJobNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .map(m -> m.getAnnotation(XxlJob.class))
                .filter(a -> a != null)
                .map(XxlJob::value)
                .toList();
    }
}
