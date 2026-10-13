package com.ccloud.module.pay.mq.message;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 退款单消息。退款终态（成功或失败）时由 pay 在同一事务内写入 outbox，订阅方按 appKey 过滤自己的单据，并用消息编号幂等
 */
@Data
@Accessors(chain = true)
public class PayRefundMessage {

    public static final String TOPIC_SUCCESS = "pay.refund.success";
    public static final String TOPIC_FAILURE = "pay.refund.failure";

    /** 单据所属租户，消费线程据此切换租户上下文 */
    private Long tenantId;
    private Long appId;
    private String appKey;
    private Long refundId;
    /** 外部退款单号 */
    private String refundNo;
    private Long orderId;
    private String merchantOrderId;
    private String merchantRefundId;
    /** 退款金额，单位分 */
    private Integer refundPrice;
    /** 退款状态，见 PayRefundStatusEnum */
    private Integer status;
    private LocalDateTime successTime;
    private String channelErrorMsg;
}
