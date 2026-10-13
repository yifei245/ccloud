package com.ccloud.module.pay.mq.message;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 转账单消息。转账终态（成功或失败）时由 pay 在同一事务内写入 outbox，订阅方按 appKey 过滤自己的单据，并用消息编号幂等
 */
@Data
@Accessors(chain = true)
public class PayTransferMessage {

    public static final String TOPIC_SUCCESS = "pay.transfer.success";
    public static final String TOPIC_FAILURE = "pay.transfer.failure";

    /** 单据所属租户，消费线程据此切换租户上下文 */
    private Long tenantId;
    private Long appId;
    private String appKey;
    private Long transferId;
    /** 外部转账单号 */
    private String transferNo;
    private String merchantTransferId;
    /** 转账金额，单位分 */
    private Integer price;
    /** 转账状态，见 PayTransferStatusEnum */
    private Integer status;
    private LocalDateTime successTime;
    private String channelErrorMsg;
}
