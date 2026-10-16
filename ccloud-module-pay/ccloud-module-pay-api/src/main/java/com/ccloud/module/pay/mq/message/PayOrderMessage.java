package com.ccloud.module.pay.mq.message;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 支付单消息。支付成功时由 pay 在同一事务内写入 outbox，订阅方按 appKey 过滤自己的单据，并用消息编号幂等
 */
@Data
@Accessors(chain = true)
public class PayOrderMessage {

    public static final String TOPIC_PAID = "pay.order.paid";

    /** 单据所属租户，消费线程据此切换租户上下文 */
    private Long tenantId;
    private Long appId;
    private String appKey;
    private Long orderId;
    /** 支付成功的外部订单号 */
    private String orderNo;
    private String merchantOrderId;
    /** 支付金额，单位分 */
    private Integer price;
    private String channelCode;
    private String payMethod;
    private LocalDateTime successTime;
    private Long userId;
    private Integer userType;
}
