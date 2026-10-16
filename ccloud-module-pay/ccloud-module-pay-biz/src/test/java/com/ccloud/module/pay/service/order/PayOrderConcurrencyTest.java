package com.ccloud.module.pay.service.order;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.api.app.OpenAppApi;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderExtensionDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderExtensionMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderMapper;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.mq.message.PayOrderMessage;
import com.ccloud.module.pay.service.app.PayAppServiceImpl;
import com.ccloud.module.pay.service.channel.PayChannelServiceImpl;
import com.ccloud.module.pay.support.FakePayClientProvider;
import com.ccloud.module.pay.test.BasePayDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 并发通知须在事务之外运行，所以单独成类；数据真实提交，结束后手工清理
 */
@Import({PayOrderServiceImpl.class, PayAppServiceImpl.class, PayChannelServiceImpl.class, PayClientFactory.class,
        FakePayClientProvider.class, PayNoRedisDAO.class, PayProperties.class})
class PayOrderConcurrencyTest extends BasePayDbAndRedisUnitTest {

    @Resource
    private PayOrderServiceImpl orderService;
    @Resource
    private PayOrderMapper orderMapper;
    @Resource
    private PayOrderExtensionMapper extensionMapper;
    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private OutboxMessageService outboxMessageService;
    @MockitoBean
    private OpenAppApi openAppApi;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 并发通知同一笔支付只入账一次() throws Exception {
        PayAppDO app = TenantUtils.execute(TENANT_ID, () -> {
            PayAppDO a = new PayAppDO().setAppKey("ut-conc").setName("并发").setStatus(0);
            appMapper.insert(a);
            return a;
        });
        PayChannelDO channel = TenantUtils.execute(TENANT_ID, () -> {
            PayChannelDO c = new PayChannelDO().setAppId(app.getId()).setCode("mock").setStatus(0).setFeeRate(0.6)
                    .setConfig("{}");
            channelMapper.insert(c);
            return c;
        });
        PayOrderExtensionDO ext = TenantUtils.execute(TENANT_ID, () -> {
            Long orderId = orderService.createOrder(new PayOrderCreateReqDTO().setAppKey("ut-conc")
                    .setMerchantOrderId("MC1").setSubject("并发商品").setPrice(100)
                    .setExpireTime(LocalDateTime.now().plusHours(1)).setUserIp("127.0.0.1")
                    .setUserId(80002L).setUserType(UserTypeEnum.MEMBER.getValue()));
            PayOrderSubmitRespVO resp = orderService.submitOrder(orderId,
                    new PayOrderSubmitReqVO().setChannelCode("mock"), "127.0.0.1");
            return extensionMapper.selectByNo(resp.getNo());
        });
        try {
            int threads = 8;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    TenantUtils.execute(TENANT_ID, () -> orderService.notifyOrder(channel,
                            PayOrderResult.success(ext.getNo(), "CH", 100, LocalDateTime.now(), "{}")));
                    return null;
                }));
            }
            ready.await();
            go.countDown();
            for (Future<?> f : futures) {
                f.get();
            }
            pool.shutdown();
            verify(outboxMessageService, times(1)).saveAndPublish(eq(PayOrderMessage.TOPIC_PAID), any());
            assertThat(TenantUtils.execute(TENANT_ID, () -> orderMapper.selectById(ext.getOrderId())).getExtensionId())
                    .isEqualTo(ext.getId());
        } finally {
            jdbcTemplate.update("DELETE FROM pay_order_extension WHERE order_id = ?", ext.getOrderId());
            jdbcTemplate.update("DELETE FROM pay_order WHERE id = ?", ext.getOrderId());
            jdbcTemplate.update("DELETE FROM pay_channel WHERE app_id = ?", app.getId());
            jdbcTemplate.update("DELETE FROM pay_app WHERE id = ?", app.getId());
        }
    }
}
