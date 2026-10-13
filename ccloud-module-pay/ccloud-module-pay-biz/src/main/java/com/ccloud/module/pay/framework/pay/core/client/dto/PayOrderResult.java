package com.ccloud.module.pay.framework.pay.core.client.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 统一下单、查询、回调解析的结果
 */
@Data
@Accessors(chain = true)
public class PayOrderResult {

    private PayClientStatus status;
    private String outTradeNo;
    private String channelOrderNo;
    /** 渠道用户编号，如微信 openid */
    private String channelUserId;
    /** 渠道确认的金额，单位分。成功时必填，用于与订单金额核对 */
    private Integer price;
    private LocalDateTime successTime;
    /** 等待用户支付时的展示方式与内容 */
    private PayDisplayMode displayMode;
    private String displayContent;
    private String errorCode;
    private String errorMsg;
    /** 渠道原始报文，存入 channel_notify_data 供对账排查 */
    private String rawData;

    public static PayOrderResult waiting(String outTradeNo, PayDisplayMode mode, String content) {
        return new PayOrderResult().setStatus(PayClientStatus.WAITING).setOutTradeNo(outTradeNo)
                .setDisplayMode(mode).setDisplayContent(content);
    }

    public static PayOrderResult success(String outTradeNo, String channelOrderNo, Integer price,
                                         LocalDateTime successTime, String rawData) {
        return new PayOrderResult().setStatus(PayClientStatus.SUCCESS).setOutTradeNo(outTradeNo)
                .setChannelOrderNo(channelOrderNo).setPrice(price).setSuccessTime(successTime)
                .setDisplayMode(PayDisplayMode.NONE).setRawData(rawData);
    }

    public static PayOrderResult failure(String outTradeNo, String errorCode, String errorMsg, String rawData) {
        return new PayOrderResult().setStatus(PayClientStatus.FAILURE).setOutTradeNo(outTradeNo)
                .setErrorCode(errorCode).setErrorMsg(errorMsg).setRawData(rawData);
    }

}
