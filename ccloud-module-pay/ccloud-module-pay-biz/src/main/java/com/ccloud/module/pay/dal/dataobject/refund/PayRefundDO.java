package com.ccloud.module.pay.dal.dataobject.refund;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 退款单。同一应用内商户退款单号唯一；创建时即在支付单上预占退款额度。按租户隔离
 */
@TableName("pay_refund")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class PayRefundDO extends TenantBaseDO {

    /**
     * 退款单编号
     */
    @TableId
    private Long id;
    /**
     * 外部退款单号，传给渠道
     */
    private String no;
    /**
     * 应用编号
     */
    private Long appId;
    /**
     * 渠道编号，即原支付所用的渠道
     */
    private Long channelId;
    /**
     * 渠道编码
     */
    private String channelCode;
    /**
     * 支付单编号
     */
    private Long orderId;
    /**
     * 支付单的外部订单号
     */
    private String orderNo;
    /**
     * 商户订单号
     */
    private String merchantOrderId;
    /**
     * 商户退款单号
     */
    private String merchantRefundId;
    /**
     * 退款状态，见 PayRefundStatusEnum
     */
    private Integer status;
    /**
     * 原支付金额，单位分
     */
    private Integer payPrice;
    /**
     * 退款金额，单位分
     */
    private Integer refundPrice;
    /**
     * 退款原因
     */
    private String reason;
    /**
     * 用户 IP
     */
    private String userIp;
    /**
     * 渠道订单号
     */
    private String channelOrderNo;
    /**
     * 渠道退款单号
     */
    private String channelRefundNo;
    /**
     * 退款成功时间
     */
    private LocalDateTime successTime;
    /**
     * 渠道错误码
     */
    private String channelErrorCode;
    /**
     * 渠道错误信息
     */
    private String channelErrorMsg;
    /**
     * 渠道回调原始报文
     */
    private String channelNotifyData;
}
