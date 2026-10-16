package com.ccloud.module.pay.framework.pay.core.client.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 统一转账、查询、回调解析的结果
 */
@Data
@Accessors(chain = true)
public class PayTransferResult {

    private PayClientStatus status;
    private String outTransferNo;
    private String channelTransferNo;
    /** 渠道确认的转账金额，单位分 */
    private Integer price;
    private LocalDateTime successTime;
    private String errorCode;
    private String errorMsg;
    /** 渠道原始报文 */
    private String rawData;

    public static PayTransferResult waiting(String outTransferNo) {
        return new PayTransferResult().setStatus(PayClientStatus.WAITING).setOutTransferNo(outTransferNo);
    }

    public static PayTransferResult success(String outTransferNo, String channelTransferNo, Integer price,
                                            LocalDateTime successTime, String rawData) {
        return new PayTransferResult().setStatus(PayClientStatus.SUCCESS).setOutTransferNo(outTransferNo)
                .setChannelTransferNo(channelTransferNo).setPrice(price).setSuccessTime(successTime)
                .setRawData(rawData);
    }

    public static PayTransferResult failure(String outTransferNo, String errorCode, String errorMsg, String rawData) {
        return new PayTransferResult().setStatus(PayClientStatus.FAILURE).setOutTransferNo(outTransferNo)
                .setErrorCode(errorCode).setErrorMsg(errorMsg).setRawData(rawData);
    }

}
