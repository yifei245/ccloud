package com.ccloud.module.pay.service.wallet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.module.open.api.app.OpenAppApi;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import com.ccloud.module.pay.controller.app.wallet.vo.AppPayWalletRechargeCreateReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.dal.dataobject.refund.PayRefundDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderMapper;
import com.ccloud.module.pay.dal.mysql.refund.PayRefundMapper;
import com.ccloud.module.pay.dal.mysql.wallet.PayWalletRechargeMapper;
import com.ccloud.module.pay.dal.mysql.wallet.PayWalletRechargePackageMapper;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.mq.message.PayOrderMessage;
import com.ccloud.module.pay.mq.message.PayRefundMessage;
import com.ccloud.module.pay.service.app.PayAppServiceImpl;
import com.ccloud.module.pay.service.channel.PayChannelServiceImpl;
import com.ccloud.module.pay.service.common.PayAfterCommitExecutor;
import com.ccloud.module.pay.service.order.PayOrderServiceImpl;
import com.ccloud.module.pay.service.refund.PayRefundServiceImpl;
import com.ccloud.module.pay.support.FakePayClientProvider;
import com.ccloud.module.pay.test.BasePayDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * 钱包充值测试的共同准备：应用 wallet（与 PayProperties 默认的充值应用标识一致）及其 mock 渠道，套餐「充 100 送 10」。
 * 会员编号落在 81000 段，套餐名以 ut- 开头，清理只动这些数据
 */
@Import({PayWalletRechargeServiceImpl.class, PayWalletRechargePackageServiceImpl.class, PayWalletServiceImpl.class,
        PayRefundServiceImpl.class, PayOrderServiceImpl.class, PayAppServiceImpl.class, PayChannelServiceImpl.class,
        PayClientFactory.class, FakePayClientProvider.class, PayNoRedisDAO.class, PayProperties.class,
        PayAfterCommitExecutor.class})
abstract class PayWalletRechargeTestBase extends BasePayDbAndRedisUnitTest {

    protected static final long MEMBER = 81001L;

    @Resource
    protected PayWalletRechargeServiceImpl rechargeService;
    @Resource
    protected PayWalletServiceImpl walletService;
    @Resource
    protected PayOrderServiceImpl orderService;
    @MockitoSpyBean
    protected PayWalletRechargeMapper rechargeMapper;
    @Resource
    protected PayWalletRechargePackageMapper packageMapper;
    @Resource
    protected PayOrderMapper orderMapper;
    @Resource
    protected PayRefundMapper refundMapper;
    @Resource
    protected PayAppMapper appMapper;
    @Resource
    protected PayChannelMapper channelMapper;
    @Resource
    protected FakePayClientProvider fakeProvider;
    @Resource
    protected JdbcTemplate jdbcTemplate;
    @MockitoBean
    protected OutboxMessageService outboxMessageService;
    @MockitoBean
    protected OpenAppApi openAppApi;

    protected PayAppDO rechargeApp;
    protected PayChannelDO channel;
    protected PayWalletRechargePackageDO pkg;

    @BeforeEach
    void setUpRechargeApp() {
        cleanUpRechargeData();
        fakeProvider.client().reset();
        rechargeApp = new PayAppDO().setAppKey("wallet").setName("钱包充值").setStatus(0);
        appMapper.insert(rechargeApp);
        channel = new PayChannelDO().setAppId(rechargeApp.getId()).setCode("mock").setStatus(0).setFeeRate(0D).setConfig("{}");
        channelMapper.insert(channel);
        pkg = new PayWalletRechargePackageDO().setName("ut-充 100 送 10").setPayPrice(10000).setBonusPrice(1000).setStatus(0);
        packageMapper.insert(pkg);
    }

    @AfterEach
    void cleanUpRechargeData() {
        String wallets = "(SELECT id FROM (SELECT id FROM pay_wallet WHERE user_id BETWEEN 81000 AND 81999) t)";
        jdbcTemplate.update("DELETE FROM pay_wallet_recharge WHERE wallet_id IN " + wallets);
        jdbcTemplate.update("DELETE FROM pay_wallet_transaction WHERE wallet_id IN " + wallets);
        jdbcTemplate.update("DELETE FROM pay_wallet WHERE user_id BETWEEN 81000 AND 81999");
        jdbcTemplate.update("DELETE r FROM pay_refund r JOIN pay_app a ON r.app_id = a.id WHERE a.app_key = 'wallet'");
        jdbcTemplate.update("DELETE e FROM pay_order_extension e JOIN pay_order o ON e.order_id = o.id "
                + "JOIN pay_app a ON o.app_id = a.id WHERE a.app_key = 'wallet'");
        jdbcTemplate.update("DELETE o FROM pay_order o JOIN pay_app a ON o.app_id = a.id WHERE a.app_key = 'wallet'");
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key = 'wallet'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key = 'wallet'");
        jdbcTemplate.update("DELETE FROM pay_wallet_recharge_package WHERE name LIKE 'ut-%'");
    }

    protected PayWalletRechargeDO recharge(Long packageId, Integer payPrice) {
        return rechargeService.createRecharge(MEMBER, UserTypeEnum.MEMBER.getValue(),
                new AppPayWalletRechargeCreateReqVO().setPackageId(packageId).setPayPrice(payPrice), "127.0.0.1");
    }

    /** 对充值的支付单提交 mock 渠道并返回支付尝试的外部单号 */
    protected String submit(PayWalletRechargeDO recharge) {
        return orderService.submitOrder(recharge.getPayOrderId(), new PayOrderSubmitReqVO().setChannelCode("mock"), "1.1.1.1").getNo();
    }

    /** 渠道通知支付成功：支付单置为支付成功（本类不经消息，入账由 paid 模拟） */
    protected void orderPaid(PayWalletRechargeDO recharge) {
        orderService.notifyOrder(channel, PayOrderResult.success(submit(recharge), "CH", recharge.getPayPrice(), LocalDateTime.now(), "{}"));
    }

    protected PayOrderMessage paidMessage(PayWalletRechargeDO recharge) {
        PayOrderDO order = orderMapper.selectById(recharge.getPayOrderId());
        return new PayOrderMessage().setTenantId(TENANT_ID).setAppId(order.getAppId())
                .setAppKey("wallet").setOrderId(order.getId()).setMerchantOrderId(order.getMerchantOrderId())
                .setPrice(order.getPrice()).setChannelCode("mock").setSuccessTime(LocalDateTime.now());
    }

    /** 模拟支付成功消息（Task 13 的消费者会这样调用） */
    protected void paid(PayWalletRechargeDO recharge) {
        rechargeService.handleRechargePaid(paidMessage(recharge));
    }

    /** 充值、支付单支付成功并入账，返回最新记录 */
    protected PayWalletRechargeDO rechargedAndPaid() {
        PayWalletRechargeDO recharge = recharge(pkg.getId(), null);
        orderPaid(recharge);
        paid(recharge);
        return rechargeMapper.selectById(recharge.getId());
    }

    /** 充值、支付、入账、发起充值退款，返回最新记录 */
    protected PayWalletRechargeDO paidAndRefunding() {
        PayWalletRechargeDO recharge = rechargedAndPaid();
        rechargeService.refundRecharge(recharge.getId(), "127.0.0.1");
        return rechargeMapper.selectById(recharge.getId());
    }

    /** 按充值的退款单组装退款结果消息 */
    protected PayRefundMessage refundMessage(PayWalletRechargeDO recharge, PayRefundStatusEnum status) {
        PayRefundDO refund = refundMapper.selectById(recharge.getPayRefundId());
        return new PayRefundMessage().setTenantId(TENANT_ID).setAppId(refund.getAppId()).setAppKey("wallet")
                .setRefundId(refund.getId()).setOrderId(refund.getOrderId()).setMerchantOrderId(refund.getMerchantOrderId())
                .setMerchantRefundId(refund.getMerchantRefundId()).setRefundPrice(refund.getRefundPrice())
                .setStatus(status.getStatus())
                .setSuccessTime(status == PayRefundStatusEnum.SUCCESS ? LocalDateTime.now() : null);
    }

    protected int balanceOf(PayWalletRechargeDO recharge) {
        return walletService.getWallet(recharge.getWalletId()).getBalance();
    }

    /** 收集服务类在 fn 执行期间输出的日志 */
    protected List<ILoggingEvent> captureLogs(Supplier<Void> fn) {
        Logger logger = (Logger) LoggerFactory.getLogger(PayWalletRechargeServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            fn.get();
        } finally {
            logger.detachAppender(appender);
        }
        return appender.list;
    }

    protected static boolean hasLog(List<ILoggingEvent> logs, Level level, String... fragments) {
        return logs.stream().anyMatch(e -> e.getLevel() == level
                && java.util.Arrays.stream(fragments).allMatch(f -> e.getFormattedMessage().contains(f)));
    }
}
