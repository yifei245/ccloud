package com.ccloud.module.pay.service.wallet;

import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.pay.controller.app.wallet.vo.AppPayWalletRechargeCreateReqVO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import com.ccloud.module.pay.mq.message.PayOrderMessage;
import com.ccloud.module.pay.mq.message.PayRefundMessage;

/**
 * 钱包充值服务。充值支付单的商户订单号为 "WR" + 充值编号，充值退款的商户退款单号为 "WR" + 充值编号 + "-R"
 */
public interface PayWalletRechargeService {

    /**
     * 创建充值记录与对应的支付单（会员本人的订单）。按套餐充值有赠送，自定义金额无赠送
     */
    PayWalletRechargeDO createRecharge(Long userId, Integer userType, AppPayWalletRechargeCreateReqVO reqVO, String userIp);

    /**
     * @param walletId 钱包编号，为空查全部（管理端）
     */
    PageResult<PayWalletRechargeDO> getRechargePage(Long walletId, PageParam pageParam);

    /**
     * 充值退款：先扣回到账总额（实付加赠送），再对原支付单退实付金额。余额不足时整体失败
     */
    void refundRecharge(Long id, String userIp);

    /**
     * 支付成功消息：入账实付加赠送。幂等
     */
    void handleRechargePaid(PayOrderMessage message);

    /**
     * 退款结果消息：成功置为退款成功，失败退回发起时扣回的金额。幂等
     */
    void handleRechargeRefunded(PayRefundMessage message);

    /**
     * 该支付应用是否为钱包充值应用
     */
    boolean isRechargeApp(Long appId);
}
