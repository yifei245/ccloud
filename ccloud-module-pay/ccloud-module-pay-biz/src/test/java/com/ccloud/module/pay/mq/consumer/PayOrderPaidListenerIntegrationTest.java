package com.ccloud.module.pay.mq.consumer;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.mq.outbox.config.CcloudOutboxAutoConfiguration;
import com.ccloud.module.open.api.notify.OpenNotifyApi;
import com.ccloud.module.open.api.notify.dto.OpenNotifySendReqDTO;
import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.service.notify.PayOpenNotifyServiceImpl;
import com.ccloud.module.pay.service.wallet.PayWalletRechargeTestBase;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 真实行为：真实监听器、真实 IdempotentConsumer（消费日志落库）、真实充值服务与钱包，只有开放平台 RPC 是桩。
 * 不在测试事务中运行，数据真实提交（消费日志与幂等的事务边界才与线上一致），开头与结尾清理
 */
@Import({PayOrderPaidListener.class, PayRefundSuccessListener.class, PayRefundFailureListener.class,
        PayOpenNotifyServiceImpl.class})
@ImportAutoConfiguration(CcloudOutboxAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PayOrderPaidListenerIntegrationTest extends PayWalletRechargeTestBase {

    private static final String MESSAGE_PREFIX = "ut13-";
    private static final long OPEN_APP_ID = 9_130_013L;

    @Resource
    private PayOrderPaidListener paidListener;
    @Resource
    private PayRefundSuccessListener refundSuccessListener;
    @Resource
    private PayRefundFailureListener refundFailureListener;
    @MockitoBean
    private OpenNotifyApi openNotifyApi;

    @BeforeEach
    void bindOpenApp() {
        cleanUpConsumeLogs();
        appMapper.updateById(new PayAppDO().setId(rechargeApp.getId()).setOpenAppId(OPEN_APP_ID));
        reset(openNotifyApi);
        when(openNotifyApi.sendNotify(any())).thenReturn(CommonResult.success(1L));
    }

    @AfterEach
    void cleanUpConsumeLogs() {
        jdbcTemplate.update("DELETE FROM infra_message_consume_log WHERE message_id LIKE ?", MESSAGE_PREFIX + "%");
    }

    private int count(String sql, Object... args) {
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    private List<String> consumers(String messageId) {
        return jdbcTemplate.queryForList("SELECT consumer FROM infra_message_consume_log WHERE message_id = ? ORDER BY id",
                String.class, messageId);
    }

    private List<OpenNotifySendReqDTO> notifies(int times) {
        ArgumentCaptor<OpenNotifySendReqDTO> captor = ArgumentCaptor.forClass(OpenNotifySendReqDTO.class);
        verify(openNotifyApi, times(times)).sendNotify(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void 支付成功消息入账一次_重复消息与不同消息编号都不会重复入账() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        orderPaid(r);

        paidListener.onMessage(MESSAGE_PREFIX + "1", paidMessage(r));
        assertThat(balanceOf(r)).isEqualTo(11000);
        assertThat(rechargeMapper.selectById(r.getId()).getPayStatus()).isTrue();
        assertThat(consumers(MESSAGE_PREFIX + "1")).containsExactly("pay-wallet-recharge", "pay-open-notify");

        // 同一条消息重投：消费日志挡住，不再入账、不再转发
        paidListener.onMessage(MESSAGE_PREFIX + "1", paidMessage(r));
        assertThat(balanceOf(r)).isEqualTo(11000);
        assertThat(count("SELECT COUNT(*) FROM pay_wallet_transaction WHERE wallet_id = ? AND biz_type = 1", r.getWalletId())).isEqualTo(1);
        notifies(1);

        // 另一个消息编号的同一订单（例如重复发出的消息）：消费日志不挡，由充值服务按业务状态幂等
        paidListener.onMessage(MESSAGE_PREFIX + "2", paidMessage(r));
        assertThat(balanceOf(r)).isEqualTo(11000);
        assertThat(walletService.getWallet(r.getWalletId()).getTotalRecharge()).isEqualTo(11000);
        assertThat(count("SELECT COUNT(*) FROM pay_wallet_transaction WHERE wallet_id = ? AND biz_type = 1", r.getWalletId())).isEqualTo(1);
        // 事件编号取各自的消息编号：开放平台按事件编号去重，两条消息各自对应一次回调
        List<OpenNotifySendReqDTO> sent = notifies(2);
        assertThat(sent).extracting(OpenNotifySendReqDTO::getEventId).containsExactly(MESSAGE_PREFIX + "1", MESSAGE_PREFIX + "2");
        assertThat(sent.get(0).getAppId()).isEqualTo(OPEN_APP_ID);
        assertThat(sent.get(0).getEvent()).isEqualTo(OpenNotifyEvents.PAY_ORDER_PAID);
        assertThat(sent.get(0).getData()).containsEntry("merchantOrderId", "WR" + r.getId()).containsEntry("status", "SUCCESS");
    }

    @Test
    void 入账成功而转发失败_重投时跳过入账并重试转发() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        orderPaid(r);
        when(openNotifyApi.sendNotify(any())).thenThrow(new IllegalStateException("开放平台不可用")).thenReturn(CommonResult.success(1L));

        assertThatThrownBy(() -> paidListener.onMessage(MESSAGE_PREFIX + "3", paidMessage(r))).isInstanceOf(IllegalStateException.class);
        // 入账已独立提交；转发的消费日志随其事务回滚，所以这条消息仍可重投
        assertThat(balanceOf(r)).isEqualTo(11000);
        assertThat(consumers(MESSAGE_PREFIX + "3")).containsExactly("pay-wallet-recharge");

        paidListener.onMessage(MESSAGE_PREFIX + "3", paidMessage(r));
        assertThat(balanceOf(r)).isEqualTo(11000);
        assertThat(count("SELECT COUNT(*) FROM pay_wallet_transaction WHERE wallet_id = ? AND biz_type = 1", r.getWalletId())).isEqualTo(1);
        assertThat(consumers(MESSAGE_PREFIX + "3")).containsExactly("pay-wallet-recharge", "pay-open-notify");
        assertThat(notifies(2)).extracting(OpenNotifySendReqDTO::getEventId).containsOnly(MESSAGE_PREFIX + "3");
    }

    @Test
    void 开放平台返回失败码时同样抛出并保留重投机会() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        orderPaid(r);
        when(openNotifyApi.sendNotify(any())).thenReturn(CommonResult.error(500, "down"));

        assertThatThrownBy(() -> paidListener.onMessage(MESSAGE_PREFIX + "4", paidMessage(r)))
                .isInstanceOf(com.ccloud.framework.common.exception.ServiceException.class);
        assertThat(consumers(MESSAGE_PREFIX + "4")).containsExactly("pay-wallet-recharge");
    }

    @Test
    void 充值退款失败消息退回一次_重复消息与不同消息编号都不会重复退回() {
        PayWalletRechargeDO r = paidAndRefunding();
        assertThat(balanceOf(r)).as("发起退款时已扣回").isZero();

        refundFailureListener.onMessage(MESSAGE_PREFIX + "r1", refundMessage(r, PayRefundStatusEnum.FAILURE));
        assertThat(balanceOf(r)).isEqualTo(11000);
        assertThat(rechargeMapper.selectById(r.getId()).getRefundStatus()).isEqualTo(PayRefundStatusEnum.FAILURE.getStatus());
        assertThat(consumers(MESSAGE_PREFIX + "r1")).containsExactly("pay-wallet-recharge", "pay-open-notify");

        refundFailureListener.onMessage(MESSAGE_PREFIX + "r1", refundMessage(r, PayRefundStatusEnum.FAILURE));
        assertThat(balanceOf(r)).isEqualTo(11000);

        refundFailureListener.onMessage(MESSAGE_PREFIX + "r2", refundMessage(r, PayRefundStatusEnum.FAILURE));
        assertThat(balanceOf(r)).isEqualTo(11000);
        assertThat(count("SELECT COUNT(*) FROM pay_wallet_transaction WHERE wallet_id = ? AND biz_type = 1 AND biz_id LIKE '%rollback'",
                r.getWalletId())).isEqualTo(1);

        List<OpenNotifySendReqDTO> sent = notifies(2);
        assertThat(sent.get(0).getEvent()).isEqualTo(OpenNotifyEvents.PAY_REFUND_FAILURE);
        assertThat(sent.get(0).getData()).containsEntry("status", "FAILURE");
    }

    @Test
    void 充值退款成功消息不动余额_只记退款成功() {
        PayWalletRechargeDO r = paidAndRefunding();

        refundSuccessListener.onMessage(MESSAGE_PREFIX + "r3", refundMessage(r, PayRefundStatusEnum.SUCCESS));
        refundSuccessListener.onMessage(MESSAGE_PREFIX + "r4", refundMessage(r, PayRefundStatusEnum.SUCCESS));

        assertThat(balanceOf(r)).isZero();
        assertThat(rechargeMapper.selectById(r.getId()).getRefundStatus()).isEqualTo(PayRefundStatusEnum.SUCCESS.getStatus());
        assertThat(notifies(2)).allSatisfy(n -> assertThat(n.getEvent()).isEqualTo(OpenNotifyEvents.PAY_REFUND_SUCCESS));
    }
}
