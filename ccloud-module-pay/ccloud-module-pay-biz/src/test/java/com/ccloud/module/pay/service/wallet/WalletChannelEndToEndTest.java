package com.ccloud.module.pay.service.wallet;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.module.open.api.app.OpenAppApi;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.ccloud.module.pay.api.transfer.dto.PayTransferCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.order.PayOrderStatusEnum;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.enums.transfer.PayTransferStatusEnum;
import com.ccloud.module.pay.enums.wallet.PayWalletBizTypeEnum;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.mq.message.PayRefundMessage;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.framework.pay.core.client.impl.wallet.WalletPayClientProvider;
import com.ccloud.module.pay.service.app.PayAppServiceImpl;
import com.ccloud.module.pay.service.channel.PayChannelServiceImpl;
import com.ccloud.module.pay.service.common.PayAfterCommitExecutor;
import com.ccloud.module.pay.service.order.PayOrderServiceImpl;
import com.ccloud.module.pay.service.refund.PayRefundServiceImpl;
import com.ccloud.module.pay.service.transfer.PayTransferServiceImpl;
import com.ccloud.module.pay.test.BasePayDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

/**
 * 经渠道层使用真实的钱包渠道（WalletPayClient），不用 Fake：钱包渠道自带本地事务，必须在外层事务提交之后、
 * 脱离已完成事务的环境中执行，才会真正提交。数据真实提交，按应用标识 ut-e2e 与会员编号 82001、82002 清理
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({PayOrderServiceImpl.class, PayRefundServiceImpl.class, PayTransferServiceImpl.class, PayAppServiceImpl.class,
        PayChannelServiceImpl.class, PayClientFactory.class, WalletPayClientProvider.class, PayWalletServiceImpl.class,
        PayNoRedisDAO.class, PayProperties.class, PayAfterCommitExecutor.class})
class WalletChannelEndToEndTest extends BasePayDbAndRedisUnitTest {

    private static final long PAYER = 82001L;
    private static final long PAYEE = 82002L;

    @Resource
    private PayOrderServiceImpl orderService;
    @Resource
    private PayRefundServiceImpl refundService;
    @Resource
    private PayTransferServiceImpl transferService;
    @Resource
    private PayWalletServiceImpl walletService;
    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private OutboxMessageService outboxMessageService;
    @MockitoBean
    private OpenAppApi openAppApi;

    @BeforeEach
    void setUpApp() {
        cleanUp();
        PayAppDO app = new PayAppDO().setAppKey("ut-e2e").setName("钱包渠道端到端").setStatus(0);
        appMapper.insert(app);
        // 渠道编码 wallet：由真实的 WalletPayClientProvider 创建客户端
        channelMapper.insert(new PayChannelDO().setAppId(app.getId()).setCode("wallet").setStatus(0).setFeeRate(0D).setConfig("{}"));
    }

    @AfterEach
    void cleanUp() {
        String wallets = "(SELECT id FROM (SELECT id FROM pay_wallet WHERE user_id IN (" + PAYER + ", " + PAYEE + ")) t)";
        jdbcTemplate.update("DELETE FROM pay_wallet_transaction WHERE wallet_id IN " + wallets);
        jdbcTemplate.update("DELETE FROM pay_wallet WHERE user_id IN (" + PAYER + ", " + PAYEE + ")");
        jdbcTemplate.update("DELETE t FROM pay_transfer t JOIN pay_app a ON t.app_id = a.id WHERE a.app_key = 'ut-e2e'");
        jdbcTemplate.update("DELETE r FROM pay_refund r JOIN pay_app a ON r.app_id = a.id WHERE a.app_key = 'ut-e2e'");
        jdbcTemplate.update("DELETE e FROM pay_order_extension e JOIN pay_order o ON e.order_id = o.id "
                + "JOIN pay_app a ON o.app_id = a.id WHERE a.app_key = 'ut-e2e'");
        jdbcTemplate.update("DELETE o FROM pay_order o JOIN pay_app a ON o.app_id = a.id WHERE a.app_key = 'ut-e2e'");
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key = 'ut-e2e'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key = 'ut-e2e'");
    }

    private int queryInt(String sql, Object... args) {
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    private int balanceOf(long userId) {
        return queryInt("SELECT balance FROM pay_wallet WHERE user_id = ? AND user_type = 1", userId);
    }

    @Test
    void 会员用钱包余额支付后_在已提交的外层事务中发起退款_余额退回且退款与订单状态正确() {
        Long walletId = walletService.getOrCreateWallet(PAYER, UserTypeEnum.MEMBER.getValue()).getId();
        walletService.addBalance(walletId, PayWalletBizTypeEnum.ADJUST, "e2e-init", "初始", 5000);
        Long orderId = orderService.createOrder(new PayOrderCreateReqDTO().setAppKey("ut-e2e").setMerchantOrderId("E2E-1")
                .setSubject("端到端").setPrice(1000).setExpireTime(LocalDateTime.now().plusHours(1)).setUserIp("1.1.1.1")
                .setUserId(PAYER).setUserType(UserTypeEnum.MEMBER.getValue()));

        orderService.submitOrder(orderId, new PayOrderSubmitReqVO().setChannelCode("wallet"), "1.1.1.1");

        assertThat(balanceOf(PAYER)).as("支付扣款").isEqualTo(4000);
        assertThat(queryInt("SELECT status FROM pay_order WHERE id = ?", orderId)).isEqualTo(PayOrderStatusEnum.SUCCESS.getStatus());

        // 退款在一个真正提交的外层事务里发起：渠道调用推迟到提交之后，钱包的本地事务不能加入已完成的外层事务。
        // MySQL 下，加入已完成事务的写入会在事务清理时因恢复自动提交而被隐式提交，仅看数据最终是否可见分辨不出，
        // 所以给外层事务命名，在退款结果回写（经渠道返回后写消息）时检查当前事务不是那个外层事务
        AtomicReference<String> txNameAtRefundPublish = new AtomicReference<>("未触发");
        doAnswer(invocation -> {
            txNameAtRefundPublish.set(String.valueOf(TransactionSynchronizationManager.getCurrentTransactionName()));
            return null;
        }).when(outboxMessageService).saveAndPublish(eq(PayRefundMessage.TOPIC_SUCCESS), any());
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        outer.setName("e2e-outer");
        Long refundId = outer.execute(status -> refundService.createRefund(
                new PayRefundCreateReqDTO().setAppKey("ut-e2e").setMerchantOrderId("E2E-1").setMerchantRefundId("E2E-1-R")
                        .setPrice(1000).setReason("端到端退款").setUserIp("1.1.1.1")));

        assertThat(txNameAtRefundPublish.get()).as("提交后的渠道调用与结果回写不在已完成的外层事务中").isEqualTo("null");
        assertThat(balanceOf(PAYER)).as("退款退回余额（已提交，独立连接可见）").isEqualTo(5000);
        assertThat(queryInt("SELECT total_expense FROM pay_wallet WHERE user_id = ?", PAYER)).as("累计支出随退款冲回").isZero();
        assertThat(queryInt("SELECT status FROM pay_refund WHERE id = ?", refundId)).isEqualTo(PayRefundStatusEnum.SUCCESS.getStatus());
        assertThat(queryInt("SELECT status FROM pay_order WHERE id = ?", orderId)).as("全额退款后订单为已退款")
                .isEqualTo(PayOrderStatusEnum.REFUND.getStatus());
        assertThat(queryInt("SELECT COUNT(*) FROM pay_wallet_transaction WHERE wallet_id = ? AND biz_type = ?", walletId,
                PayWalletBizTypeEnum.PAYMENT_REFUND.getType())).isEqualTo(1);
    }

    @Test
    void 转账到钱包账号_会员编号为收款账号_目标钱包到账且转账单成功() {
        Long transferId = transferService.createTransfer(new PayTransferCreateReqDTO().setAppKey("ut-e2e")
                .setChannelCode("wallet").setMerchantTransferId("E2E-T1").setSubject("红包").setPrice(700)
                .setUserAccount(String.valueOf(PAYEE)).setUserIp("1.1.1.1"));

        assertThat(balanceOf(PAYEE)).as("目标钱包首次开通并到账").isEqualTo(700);
        assertThat(queryInt("SELECT status FROM pay_transfer WHERE id = ?", transferId)).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
        assertThat(queryInt("SELECT COUNT(*) FROM pay_wallet_transaction t JOIN pay_wallet w ON t.wallet_id = w.id "
                + "WHERE w.user_id = ? AND t.biz_type = ?", PAYEE, PayWalletBizTypeEnum.TRANSFER_IN.getType())).isEqualTo(1);

        // 同样在已提交的外层事务中发起
        Long inTx = new TransactionTemplate(transactionManager).execute(status -> transferService.createTransfer(
                new PayTransferCreateReqDTO().setAppKey("ut-e2e").setChannelCode("wallet").setMerchantTransferId("E2E-T2")
                        .setSubject("红包二").setPrice(300).setUserAccount(String.valueOf(PAYEE)).setUserIp("1.1.1.1")));
        assertThat(balanceOf(PAYEE)).isEqualTo(1000);
        assertThat(queryInt("SELECT status FROM pay_transfer WHERE id = ?", inTx)).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
    }
}
