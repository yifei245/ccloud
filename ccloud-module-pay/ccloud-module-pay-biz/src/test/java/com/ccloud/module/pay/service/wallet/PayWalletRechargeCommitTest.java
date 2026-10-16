package com.ccloud.module.pay.service.wallet;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundResult;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.PAY_REFUND_PRICE_EXCEED;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_RECHARGE_REFUND_STATUS_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 需要真实提交的充值退款用例：并发、提交后才调用渠道、失败回滚。不在测试事务中运行，数据真实提交，开头与结尾清理
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PayWalletRechargeCommitTest extends PayWalletRechargeTestBase {

    private int count(String sql, Object... args) {
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    @Test
    void 并发充值退款只成功一次_钱包只扣一次_只有一条退款单() throws Exception {
        PayWalletRechargeDO r = rechargedAndPaid();
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return TenantUtils.execute(TENANT_ID, () -> {
                    try {
                        rechargeService.refundRecharge(r.getId(), "127.0.0.1");
                        return "OK";
                    } catch (ServiceException e) {
                        return e.getCode();
                    }
                });
            }));
        }
        ready.await();
        go.countDown();
        List<Object> results = new ArrayList<>();
        for (Future<Object> f : futures) {
            results.add(f.get());
        }
        pool.shutdown();

        assertThat(results).filteredOn("OK"::equals).as("恰好一个线程成功").hasSize(1);
        assertThat(results).filteredOn(x -> !"OK".equals(x)).as("其余都因加锁后读到已发起退款而被拒")
                .hasSize(threads - 1).containsOnly(WALLET_RECHARGE_REFUND_STATUS_ERROR.getCode());
        assertThat(count("SELECT balance FROM pay_wallet WHERE id = ?", r.getWalletId())).as("钱包只扣一次").isZero();
        assertThat(count("SELECT COUNT(*) FROM pay_wallet_transaction WHERE wallet_id = ? AND biz_type = 2", r.getWalletId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM pay_refund r JOIN pay_app a ON r.app_id = a.id WHERE a.app_key = 'wallet'")).isEqualTo(1);
        assertThat(rechargeMapper.selectById(r.getId()).getPayRefundId()).isNotNull();
    }

    @Test
    void 渠道在充值退款事务提交之后才被调用_调用时不处于任何事务_退款单与扣款已可见() {
        PayWalletRechargeDO r = rechargedAndPaid();
        AtomicInteger calls = new AtomicInteger();
        AtomicBoolean inTransaction = new AtomicBoolean(true);
        AtomicBoolean refundRowVisible = new AtomicBoolean();
        AtomicBoolean debitVisible = new AtomicBoolean();
        AtomicBoolean rechargeLinkedVisible = new AtomicBoolean();
        fakeProvider.client().onUnifiedRefund = req -> {
            calls.incrementAndGet();
            inTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            // 渠道调用线程上没有事务，JdbcTemplate 走独立连接：能看到的只有已提交的数据
            refundRowVisible.set(count("SELECT COUNT(*) FROM pay_refund WHERE no = ?", req.getOutRefundNo()) == 1);
            debitVisible.set(count("SELECT balance FROM pay_wallet WHERE id = ?", r.getWalletId()) == 0);
            rechargeLinkedVisible.set(count("SELECT COUNT(*) FROM pay_wallet_recharge WHERE id = ? AND pay_refund_id IS NOT NULL", r.getId()) == 1);
            return PayRefundResult.waiting(req.getOutRefundNo());
        };

        rechargeService.refundRecharge(r.getId(), "127.0.0.1");

        assertThat(calls.get()).as("提交后调用渠道一次").isEqualTo(1);
        assertThat(inTransaction.get()).as("渠道调用时不应有事务").isFalse();
        assertThat(refundRowVisible.get()).as("调用渠道时退款单已提交").isTrue();
        assertThat(debitVisible.get()).as("调用渠道时钱包扣款已提交").isTrue();
        assertThat(rechargeLinkedVisible.get()).as("调用渠道时充值已关联退款单").isTrue();
    }

    @Test
    void 创建退款单失败时钱包扣款随事务回滚_不调用渠道() {
        PayWalletRechargeDO r = rechargedAndPaid();
        // 让原支付单的可退额度已用尽，createRefund 在预占额度时抛 PRICE_EXCEED
        jdbcTemplate.update("UPDATE pay_order SET refund_price = price WHERE id = ?", r.getPayOrderId());

        assertThatThrownBy(() -> rechargeService.refundRecharge(r.getId(), "127.0.0.1"))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(PAY_REFUND_PRICE_EXCEED.getCode());

        assertThat(count("SELECT balance FROM pay_wallet WHERE id = ?", r.getWalletId())).as("扣款已回滚").isEqualTo(11000);
        assertThat(count("SELECT total_recharge FROM pay_wallet WHERE id = ?", r.getWalletId())).isEqualTo(11000);
        assertThat(count("SELECT COUNT(*) FROM pay_wallet_transaction WHERE wallet_id = ? AND biz_type = 2", r.getWalletId())).isZero();
        assertThat(count("SELECT COUNT(*) FROM pay_refund r JOIN pay_app a ON r.app_id = a.id WHERE a.app_key = 'wallet'")).isZero();
        assertThat(rechargeMapper.selectById(r.getId()).getPayRefundId()).isNull();
        assertThat(fakeProvider.client().lastRefundReq).isNull();
    }
}
