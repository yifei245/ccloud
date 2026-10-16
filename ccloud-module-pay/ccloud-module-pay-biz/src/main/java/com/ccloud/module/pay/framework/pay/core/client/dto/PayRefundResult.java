package com.ccloud.module.pay.framework.pay.core.client.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 统一退款、查询、回调解析的结果
 */
@Data
@Accessors(chain = true)
public class PayRefundResult {

    private PayClientStatus status;
    private String outRefundNo;
    private String channelRefundNo;
    /** 渠道确认的退款金额，单位分 */
    private Integer refundPrice;
    private LocalDateTime successTime;
    private String errorCode;
    private String errorMsg;
    /** 渠道原始报文 */
    private String rawData;

    public static PayRefundResult waiting(String outRefundNo) {
        return new PayRefundResult().setStatus(PayClientStatus.WAITING).setOutRefundNo(outRefundNo);
    }

    public static PayRefundResult success(String outRefundNo, String channelRefundNo, Integer refundPrice,
                                          LocalDateTime successTime, String rawData) {
        return new PayRefundResult().setStatus(PayClientStatus.SUCCESS).setOutRefundNo(outRefundNo)
                .setChannelRefundNo(channelRefundNo).setRefundPrice(refundPrice).setSuccessTime(successTime)
                .setRawData(rawData);
    }

    public static PayRefundResult failure(String outRefundNo, String errorCode, String errorMsg, String rawData) {
        return new PayRefundResult().setStatus(PayClientStatus.FAILURE).setOutRefundNo(outRefundNo)
                .setErrorCode(errorCode).setErrorMsg(errorMsg).setRawData(rawData);
    }

}
