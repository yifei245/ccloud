package com.ccloud.module.pay.dal.dataobject.order;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 支付单。同一应用内商户订单号唯一；渠道、支付方式、手续费在支付成功时写入。按租户隔离
 */
@TableName("pay_order")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class PayOrderDO extends TenantBaseDO {

    /**
     * 支付单编号
     */
    @TableId
    private Long id;
    /**
     * 应用编号
     */
    private Long appId;
    /**
     * 渠道编号，支付成功后写入
     */
    private Long channelId;
    /**
     * 渠道编码，支付成功后写入
     */
    private String channelCode;
    /**
     * 支付方式，聚合渠道下的具体方式
     */
    private String payMethod;
    /**
     * 商户订单号
     */
    private String merchantOrderId;
    /**
     * 商品标题
     */
    private String subject;
    /**
     * 商品描述
     */
    private String body;
    /**
     * 支付金额，单位分
     */
    private Integer price;
    /**
     * 渠道手续费率，单位百分比
     */
    private Double channelFeeRate;
    /**
     * 渠道手续费，单位分
     */
    private Integer channelFeePrice;
    /**
     * 支付状态，见 PayOrderStatusEnum
     */
    private Integer status;
    /**
     * 用户 IP
     */
    private String userIp;
    /**
     * 失效时间
     */
    private LocalDateTime expireTime;
    /**
     * 支付成功时间
     */
    private LocalDateTime successTime;
    /**
     * 支付成功的支付尝试编号
     */
    private Long extensionId;
    /**
     * 支付成功的外部订单号
     */
    private String no;
    /**
     * 累计退款金额（含预占），单位分
     */
    private Integer refundPrice;
    /**
     * 渠道用户编号
     */
    private String channelUserId;
    /**
     * 渠道订单号
     */
    private String channelOrderNo;
    /**
     * 付款用户编号，外部系统创建的订单为空
     */
    private Long userId;
    /**
     * 付款用户类型，见 UserTypeEnum
     */
    private Integer userType;
}
