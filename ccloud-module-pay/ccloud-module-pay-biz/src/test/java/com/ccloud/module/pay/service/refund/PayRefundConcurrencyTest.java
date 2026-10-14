package com.ccloud.module.pay.service.refund;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.service.app.PayAppServiceImpl;
import com.ccloud.module.pay.service.channel.PayChannelServiceImpl;
import com.ccloud.module.pay.service.order.PayOrderServiceImpl;
import com.ccloud.module.pay.support.FakePayClientProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Import({PayRefundServiceImpl.class, PayOrderServiceImpl.class, PayAppServiceImpl.class, PayChannelServiceImpl.class,
        PayClientFactory.class, FakePayClientProvider.class, PayNoRedisDAO.class, PayProperties.class})
class PayRefundConcurrencyTest extends PayRefundTestBase {

    @Test
    void 并发退款合计不超过支付金额() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        CountDownLatch ready = new CountDownLatch(6);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String merchantRefundId = "RC" + i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                TenantUtils.execute(TENANT_ID, () -> {
                    try {
                        refundService.createRefund(refundReq(merchantRefundId, 300));
                    } catch (ServiceException ignored) {
                        // 额度不足或已有退款在处理中
                    }
                });
                return null;
            }));
        }
        ready.await();
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        PayOrderDO order = TenantUtils.execute(TENANT_ID, () -> orderMapper.selectById(paidOrderId));
        assertThat(order.getRefundPrice()).isLessThanOrEqualTo(1000);
        Integer reserved = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(refund_price), 0) FROM pay_refund WHERE order_id = ?",
                Integer.class, paidOrderId);
        assertThat(reserved).isEqualTo(order.getRefundPrice());
    }
}
