package com.ccloud.module.pay.service.wallet;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import com.ccloud.module.pay.dal.mysql.wallet.PayWalletMapper;
import com.ccloud.module.pay.dal.mysql.wallet.PayWalletTransactionMapper;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.wallet.PayWalletBizTypeEnum;
import com.ccloud.module.pay.test.BasePayDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_ADJUST_PRICE_ZERO;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_BALANCE_NOT_ENOUGH;
import static org.assertj.core.api.Assertions.assertThat;

@Import({PayWalletServiceImpl.class, PayNoRedisDAO.class})
class PayWalletServiceImplTest extends BasePayDbAndRedisUnitTest {

    @Resource
    private PayWalletServiceImpl walletService;
    @Resource
    private PayWalletMapper walletMapper;
    @Resource
    private PayWalletTransactionMapper transactionMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    private PayWalletDO walletWithBalance(long userId, int balance) {
        PayWalletDO wallet = walletService.getOrCreateWallet(userId, UserTypeEnum.MEMBER.getValue());
        if (balance > 0) {
            walletService.addBalance(wallet.getId(), PayWalletBizTypeEnum.ADJUST, "init-" + userId, "初始", balance);
        }
        return walletMapper.selectById(wallet.getId());
    }

    @Test
    void 首次访问自动开通钱包_再次访问返回同一个() {
        PayWalletDO a = walletService.getOrCreateWallet(70001L, 1);
        PayWalletDO b = walletService.getOrCreateWallet(70001L, 1);
        assertThat(b.getId()).isEqualTo(a.getId());
        assertThat(a.getBalance()).isZero();
    }

    @Test
    void 余额不足时不扣成负数且不写流水() {
        PayWalletDO wallet = walletWithBalance(70002L, 100);
        assertServiceException(() -> walletService.reduceBalance(wallet.getId(), PayWalletBizTypeEnum.PAYMENT, "P1", "支付", 101),
                WALLET_BALANCE_NOT_ENOUGH);
        assertThat(walletMapper.selectById(wallet.getId()).getBalance()).isEqualTo(100);
        assertThat(transactionMapper.selectByBiz(PayWalletBizTypeEnum.PAYMENT.getType(), "P1")).isNull();
    }

    @Test
    void 同一业务单据重复入账只记一次() {
        PayWalletDO wallet = walletWithBalance(70003L, 0);
        PayWalletTransactionDO a = walletService.addBalance(wallet.getId(), PayWalletBizTypeEnum.RECHARGE, "WR1", "充值", 500);
        PayWalletTransactionDO b = walletService.addBalance(wallet.getId(), PayWalletBizTypeEnum.RECHARGE, "WR1", "充值", 500);
        assertThat(b.getId()).isEqualTo(a.getId());
        PayWalletDO after = walletMapper.selectById(wallet.getId());
        assertThat(after.getBalance()).isEqualTo(500);
        assertThat(after.getTotalRecharge()).isEqualTo(500);
    }

    @Test
    void 支付累计支出_支付退款冲减支出_流水记录变更后余额() {
        PayWalletDO wallet = walletWithBalance(70004L, 1000);
        PayWalletTransactionDO pay = walletService.reduceBalance(wallet.getId(), PayWalletBizTypeEnum.PAYMENT, "P2", "支付", 300);
        assertThat(pay.getPrice()).isEqualTo(-300);
        assertThat(pay.getBalance()).isEqualTo(700);
        walletService.addBalance(wallet.getId(), PayWalletBizTypeEnum.PAYMENT_REFUND, "R2", "退款", 100);
        PayWalletDO after = walletMapper.selectById(wallet.getId());
        assertThat(after.getBalance()).isEqualTo(800);
        assertThat(after.getTotalExpense()).isEqualTo(200);
    }

    @Test
    void 后台调整金额为0报错_减少超过余额报错() {
        PayWalletDO wallet = walletWithBalance(70005L, 50);
        assertServiceException(() -> walletService.adjustBalance(wallet.getId(), 0, "x"), WALLET_ADJUST_PRICE_ZERO);
        assertServiceException(() -> walletService.adjustBalance(wallet.getId(), -51, "x"), WALLET_BALANCE_NOT_ENOUGH);
        walletService.adjustBalance(wallet.getId(), -50, "扣回");
        assertThat(walletMapper.selectById(wallet.getId()).getBalance()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 并发扣款只成功到余额为止() throws Exception {
        long userId = 70006L;
        Long walletId = TenantUtils.execute(TENANT_ID, () -> walletWithBalance(userId, 500).getId());
        try {
            ExecutorService pool = Executors.newFixedThreadPool(10);
            AtomicInteger ok = new AtomicInteger();
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                String bizId = "PC" + i;
                futures.add(pool.submit(() -> TenantUtils.execute(TENANT_ID, () -> {
                    try {
                        walletService.reduceBalance(walletId, PayWalletBizTypeEnum.PAYMENT, bizId, "支付", 100);
                        ok.incrementAndGet();
                    } catch (ServiceException ignored) {
                        // 余额不足
                    }
                })));
            }
            for (Future<?> f : futures) f.get();
            pool.shutdown();
            assertThat(ok).hasValue(5);
            assertThat(TenantUtils.execute(TENANT_ID, () -> walletMapper.selectById(walletId)).getBalance()).isZero();
        } finally {
            jdbcTemplate.update("DELETE FROM pay_wallet_transaction WHERE wallet_id = ?", walletId);
            jdbcTemplate.update("DELETE FROM pay_wallet WHERE id = ?", walletId);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 并发首次访问只开通一个钱包() throws Exception {
        long userId = 70007L;
        try {
            ExecutorService pool = Executors.newFixedThreadPool(6);
            Set<Long> ids = ConcurrentHashMap.newKeySet();
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                futures.add(pool.submit(() -> TenantUtils.execute(TENANT_ID,
                        () -> ids.add(walletService.getOrCreateWallet(userId, 1).getId()))));
            }
            for (Future<?> f : futures) f.get();
            pool.shutdown();
            assertThat(ids).hasSize(1);
        } finally {
            jdbcTemplate.update("DELETE FROM pay_wallet WHERE user_id = ?", userId);
        }
    }
}
