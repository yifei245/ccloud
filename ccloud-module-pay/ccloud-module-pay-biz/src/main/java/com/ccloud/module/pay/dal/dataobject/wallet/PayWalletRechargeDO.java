package com.ccloud.module.pay.dal.dataobject.wallet;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 钱包充值记录。支付单的商户订单号为 "WR" + 充值编号，充值退款的商户退款单号为 "WR" + 充值编号 + "-R"。按租户隔离
 */
@TableName("pay_wallet_recharge")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class PayWalletRechargeDO extends TenantBaseDO {

    /**
     * 充值编号
     */
    @TableId
    private Long id;
    /**
     * 钱包编号
     */
    private Long walletId;
    /**
     * 到账总金额（实付加赠送），单位分
     */
    private Integer totalPrice;
    /**
     * 实际支付金额，单位分
     */
    private Integer payPrice;
    /**
     * 赠送金额，单位分
     */
    private Integer bonusPrice;
    /**
     * 充值套餐编号，自定义金额时为空
     */
    private Long packageId;
    /**
     * 是否已支付
     */
    private Boolean payStatus;
    /**
     * 支付单编号
     */
    private Long payOrderId;
    /**
     * 支付渠道编码
     */
    private String payChannelCode;
    /**
     * 支付成功时间
     */
    private LocalDateTime payTime;
    /**
     * 退款单编号，非空即已发起充值退款
     */
    private Long payRefundId;
    /**
     * 退款扣回的到账总金额，单位分
     */
    private Integer refundTotalPrice;
    /**
     * 退款的实付金额，单位分
     */
    private Integer refundPayPrice;
    /**
     * 退款扣回的赠送金额，单位分
     */
    private Integer refundBonusPrice;
    /**
     * 退款成功时间
     */
    private LocalDateTime refundTime;
    /**
     * 退款状态，见 PayRefundStatusEnum：未退款或退款中为 WAITING（以 payRefundId 区分），退款成功、退款失败
     */
    private Integer refundStatus;
}
