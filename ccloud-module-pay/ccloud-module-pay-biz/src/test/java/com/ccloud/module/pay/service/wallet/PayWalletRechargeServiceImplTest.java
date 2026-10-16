package com.ccloud.module.pay.service.wallet;

import ch.qos.logback.classic.Level;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.module.pay.controller.app.wallet.vo.AppPayWalletRechargeCreateReqVO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.dal.dataobject.refund.PayRefundDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.enums.wallet.PayWalletBizTypeEnum;
import com.ccloud.module.pay.mq.message.PayOrderMessage;
import com.ccloud.module.pay.mq.message.PayRefundMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * 本类在测试事务中运行并回滚；需要真实提交的（并发、提交后调用渠道）见 PayWalletRechargeCommitTest
 */
class PayWalletRechargeServiceImplTest extends PayWalletRechargeTestBase {

    @Test
    void 未建充值应用时提示未开通() {
        appMapper.deleteById(rechargeApp.getId());
        assertServiceException(() -> recharge(pkg.getId(), null), WALLET_RECHARGE_NOT_OPENED);
    }

    @Test
    void 按套餐充值_写充值记录并创建会员本人的支付单() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        assertThat(r.getPayPrice()).isEqualTo(10000);
        assertThat(r.getBonusPrice()).isEqualTo(1000);
        assertThat(r.getTotalPrice()).isEqualTo(11000);
        PayOrderDO order = orderMapper.selectById(r.getPayOrderId());
        assertThat(order.getMerchantOrderId()).isEqualTo("WR" + r.getId());
        assertThat(order.getPrice()).isEqualTo(10000);
        assertThat(order.getUserId()).isEqualTo(MEMBER);
        assertThat(order.getUserType()).isEqualTo(UserTypeEnum.MEMBER.getValue());
        assertThat(rechargeMapper.selectById(r.getId()).getPayOrderId()).isEqualTo(order.getId());
    }

    @Test
    void 自定义金额无赠送_既无套餐也无金额报错_套餐关闭报错() {
        assertThat(recharge(null, 500).getBonusPrice()).isZero();
        assertServiceException(() -> recharge(null, null), WALLET_RECHARGE_PRICE_INVALID);
        assertServiceException(() -> recharge(null, 0), WALLET_RECHARGE_PRICE_INVALID);
        packageMapper.updateById(new PayWalletRechargePackageDO().setId(pkg.getId()).setStatus(1));
        assertServiceException(() -> recharge(pkg.getId(), null), WALLET_RECHARGE_PACKAGE_NOT_FOUND);
    }

    @Test
    void 充值应用的标识是配置的充值应用() {
        assertThat(rechargeService.isRechargeApp(rechargeApp.getId())).isTrue();
        assertThat(rechargeService.isRechargeApp(-1L)).isFalse();
    }

    @Test
    void 支付成功后入账实付加赠送_重复消息只入账一次() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        paid(r);
        paid(r);
        assertThat(balanceOf(r)).isEqualTo(11000);
        assertThat(rechargeMapper.selectById(r.getId()).getPayStatus()).isTrue();
        assertThat(walletService.getWallet(r.getWalletId()).getTotalRecharge()).isEqualTo(11000);
    }

    @Test
    void 入账金额与支付金额不一致时不入账() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        rechargeService.handleRechargePaid(paidMessage(r).setPrice(1));
        assertThat(balanceOf(r)).isZero();
        assertThat(rechargeMapper.selectById(r.getId()).getPayStatus()).isFalse();
    }

    @Test
    void 商户订单号不是充值单号时只记警告不抛异常_不入账() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        for (String bad : new String[]{null, "", "X" + r.getId(), "WR", "WRabc", "WR0", "WR-5", "WR" + r.getId() + "x",
                "WR99999999999999999999", "wr" + r.getId()}) {
            var logs = captureLogs(() -> {
                rechargeService.handleRechargePaid(paidMessage(r).setMerchantOrderId(bad));
                return null;
            });
            assertThat(hasLog(logs, Level.WARN, "不是充值单号")).as("商户订单号 %s 记警告", bad).isTrue();
        }
        assertThat(balanceOf(r)).isZero();
        assertThat(rechargeMapper.selectById(r.getId()).getPayStatus()).isFalse();
    }

    @Test
    void 消息的支付单与充值关联的支付单不一致时记错误且不入账() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        PayWalletRechargeDO other = recharge(pkg.getId(), null);
        // 另一笔充值的支付单恰好用了本充值的商户订单号（例如别的应用同名订单）
        PayOrderMessage forged = paidMessage(r).setOrderId(other.getPayOrderId());

        var logs = captureLogs(() -> {
            rechargeService.handleRechargePaid(forged);
            return null;
        });

        assertThat(hasLog(logs, Level.ERROR, String.valueOf(r.getId()), "不一致")).isTrue();
        assertThat(balanceOf(r)).isZero();
        PayWalletRechargeDO after = rechargeMapper.selectById(r.getId());
        assertThat(after.getPayStatus()).isFalse();
        assertThat(after.getPayOrderId()).isEqualTo(r.getPayOrderId());
        // 正确的消息仍可入账
        paid(r);
        assertThat(balanceOf(r)).isEqualTo(11000);
    }

    @Test
    void 充值退款_扣回到账总额_对原支付单退实付金额() {
        PayWalletRechargeDO r = rechargedAndPaid();

        rechargeService.refundRecharge(r.getId(), "127.0.0.1");

        assertThat(balanceOf(r)).isZero();
        assertThat(walletService.getWallet(r.getWalletId()).getTotalRecharge()).as("累计充值随扣回减少").isZero();
        PayWalletRechargeDO after = rechargeMapper.selectById(r.getId());
        assertThat(after.getPayRefundId()).isNotNull();
        assertThat(after.getRefundTotalPrice()).isEqualTo(11000);
        assertThat(after.getRefundPayPrice()).isEqualTo(10000);
        assertThat(after.getRefundBonusPrice()).isEqualTo(1000);
        PayRefundDO refund = refundMapper.selectById(after.getPayRefundId());
        assertThat(refund.getRefundPrice()).isEqualTo(10000);
        assertThat(refund.getMerchantRefundId()).isEqualTo("WR" + r.getId() + "-R");
        assertServiceException(() -> rechargeService.refundRecharge(r.getId(), "127.0.0.1"), WALLET_RECHARGE_REFUND_STATUS_ERROR);
    }

    @Test
    void 余额已被花掉时不能充值退款_未支付的充值不能退款() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        assertServiceException(() -> rechargeService.refundRecharge(r.getId(), "127.0.0.1"), WALLET_RECHARGE_REFUND_STATUS_ERROR);
        assertServiceException(() -> rechargeService.refundRecharge(-1L, "127.0.0.1"), WALLET_RECHARGE_NOT_FOUND);
        orderPaid(r);
        paid(r);
        walletService.reduceBalance(r.getWalletId(), PayWalletBizTypeEnum.PAYMENT, "P-spent", "消费", 5000);
        assertServiceException(() -> rechargeService.refundRecharge(r.getId(), "127.0.0.1"), WALLET_BALANCE_NOT_ENOUGH);
        assertThat(rechargeMapper.selectById(r.getId()).getPayRefundId()).isNull();
    }

    @Test
    void 退款成功置为退款成功_退款失败退回扣除的金额() {
        PayWalletRechargeDO ok = paidAndRefunding();
        rechargeService.handleRechargeRefunded(refundMessage(ok, PayRefundStatusEnum.SUCCESS));
        PayWalletRechargeDO okAfter = rechargeMapper.selectById(ok.getId());
        assertThat(okAfter.getRefundStatus()).isEqualTo(PayRefundStatusEnum.SUCCESS.getStatus());
        assertThat(okAfter.getRefundTime()).isNotNull();

        PayWalletRechargeDO fail = paidAndRefunding();
        int before = balanceOf(fail);
        rechargeService.handleRechargeRefunded(refundMessage(fail, PayRefundStatusEnum.FAILURE));
        rechargeService.handleRechargeRefunded(refundMessage(fail, PayRefundStatusEnum.FAILURE));
        assertThat(rechargeMapper.selectById(fail.getId()).getRefundStatus()).isEqualTo(PayRefundStatusEnum.FAILURE.getStatus());
        assertThat(balanceOf(fail)).as("扣回的 11000 退回且只退一次").isEqualTo(before + 11000);
        assertThat(walletService.getWallet(fail.getWalletId()).getTotalRecharge()).as("累计充值随退回恢复").isEqualTo(11000);
    }

    @Test
    void 退款结果的商户退款单号不是充值退款单号时只记警告不抛异常() {
        PayWalletRechargeDO r = paidAndRefunding();
        int before = balanceOf(r);
        for (String bad : new String[]{null, "", "XX" + r.getId() + "-R", "WR-R", "WRabc-R", "WR0-R", "WR-5-R",
                "WR99999999999999999999-R", "WR" + r.getId(), "WR" + r.getId() + "-X"}) {
            var logs = captureLogs(() -> {
                rechargeService.handleRechargeRefunded(refundMessage(r, PayRefundStatusEnum.FAILURE).setMerchantRefundId(bad));
                return null;
            });
            assertThat(hasLog(logs, Level.WARN, "不是充值退款单号")).as("商户退款单号 %s 记警告", bad).isTrue();
        }
        assertThat(balanceOf(r)).isEqualTo(before);
        assertThat(rechargeMapper.selectById(r.getId()).getRefundStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
    }

    @Test
    void 消息的退款单与充值关联的退款单不一致时记错误且不改状态_不退回金额() {
        PayWalletRechargeDO r = paidAndRefunding();
        int before = balanceOf(r);
        PayRefundMessage forged = refundMessage(r, PayRefundStatusEnum.FAILURE).setRefundId(r.getPayRefundId() + 1_000_000);

        var logs = captureLogs(() -> {
            rechargeService.handleRechargeRefunded(forged);
            return null;
        });

        assertThat(hasLog(logs, Level.ERROR, String.valueOf(r.getId()), "不一致")).isTrue();
        assertThat(rechargeMapper.selectById(r.getId()).getRefundStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
        assertThat(balanceOf(r)).isEqualTo(before);
        // 正确的消息仍生效
        rechargeService.handleRechargeRefunded(refundMessage(r, PayRefundStatusEnum.FAILURE));
        assertThat(balanceOf(r)).isEqualTo(before + 11000);
    }

    @Test
    void 并发的重复入账消息读到的都是未支付_条件更新不覆盖先到者写入的支付信息() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        PayWalletRechargeDO staleUnpaid = rechargeMapper.selectById(r.getId());
        paid(r);
        PayWalletRechargeDO first = rechargeMapper.selectById(r.getId());
        // 后到的消息在先到者提交之前读过一次，读到的仍是未支付；它携带的渠道与时间不同
        doReturn(staleUnpaid).when(rechargeMapper).selectById(r.getId());

        rechargeService.handleRechargePaid(paidMessage(r).setChannelCode("other-channel")
                .setSuccessTime(LocalDateTime.now().plusHours(1)));

        Mockito.reset(rechargeMapper);
        PayWalletRechargeDO after = rechargeMapper.selectById(r.getId());
        assertThat(after.getPayChannelCode()).as("条件更新影响 0 行，不覆盖先到者").isEqualTo(first.getPayChannelCode()).isEqualTo("mock");
        assertThat(after.getPayTime()).isEqualTo(first.getPayTime());
        assertThat(balanceOf(r)).isEqualTo(11000);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_wallet_transaction WHERE wallet_id = ? AND biz_type = ?",
                Integer.class, r.getWalletId(), PayWalletBizTypeEnum.RECHARGE.getType())).isEqualTo(1);
    }

    @Test
    void 退款消息状态是等待或空或未知时不动余额_之后的成功消息正常完成_不会重复出款() {
        PayWalletRechargeDO r = paidAndRefunding();
        int debited = balanceOf(r);

        rechargeService.handleRechargeRefunded(refundMessage(r, PayRefundStatusEnum.WAITING));
        rechargeService.handleRechargeRefunded(refundMessage(r, PayRefundStatusEnum.WAITING).setStatus(null));
        var logs = captureLogs(() -> {
            rechargeService.handleRechargeRefunded(refundMessage(r, PayRefundStatusEnum.WAITING).setStatus(99));
            return null;
        });
        assertThat(hasLog(logs, Level.WARN, String.valueOf(r.getId()), "忽略")).isTrue();
        assertThat(balanceOf(r)).as("非成功非失败的消息不退回金额").isEqualTo(debited);
        assertThat(rechargeMapper.selectById(r.getId()).getRefundStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());

        rechargeService.handleRechargeRefunded(refundMessage(r, PayRefundStatusEnum.SUCCESS));

        assertThat(rechargeMapper.selectById(r.getId()).getRefundStatus()).isEqualTo(PayRefundStatusEnum.SUCCESS.getStatus());
        assertThat(balanceOf(r)).as("退款成功后余额保持已扣回").isEqualTo(debited);
        // 成功后即使再收到失败消息也不退回（状态已不是等待）
        rechargeService.handleRechargeRefunded(refundMessage(r, PayRefundStatusEnum.FAILURE));
        assertThat(balanceOf(r)).isEqualTo(debited);
    }

    @Test
    void 充值记录的条件更新只在预期状态下生效() {
        PayWalletRechargeDO r = recharge(pkg.getId(), null);
        // 未支付不能发起退款，也不能置为退款结束
        assertThat(rechargeMapper.updateWhenRefunding(r.getId(), new PayWalletRechargeDO().setPayRefundId(1L))).isZero();
        assertThat(rechargeMapper.updateWhenRefundFinished(r.getId(), new PayWalletRechargeDO().setRefundStatus(10))).isZero();
        // 未支付 → 已支付只能一次
        assertThat(rechargeMapper.updateWhenPaid(r.getId(), new PayWalletRechargeDO().setPayStatus(true))).isEqualTo(1);
        assertThat(rechargeMapper.updateWhenPaid(r.getId(), new PayWalletRechargeDO().setPayStatus(true))).isZero();
        // 已支付 → 退款中只能一次
        assertThat(rechargeMapper.updateWhenRefunding(r.getId(), new PayWalletRechargeDO().setPayRefundId(1L))).isEqualTo(1);
        assertThat(rechargeMapper.updateWhenRefunding(r.getId(), new PayWalletRechargeDO().setPayRefundId(2L))).isZero();
        // 退款中 → 退款结束只能一次
        assertThat(rechargeMapper.updateWhenRefundFinished(r.getId(), new PayWalletRechargeDO().setRefundStatus(20))).isEqualTo(1);
        assertThat(rechargeMapper.updateWhenRefundFinished(r.getId(), new PayWalletRechargeDO().setRefundStatus(10))).isZero();
        assertThat(rechargeMapper.selectById(r.getId()).getRefundStatus()).isEqualTo(20);
    }
}
