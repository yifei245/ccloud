package com.ccloud.module.pay.framework.pay.core.client.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统一退款请求
 */
@Data
@Accessors(chain = true)
public class PayRefundUnifiedReq {

    /** 原支付的外部单号 */
    private String outTradeNo;
    /** 退款单号 */
    private String outRefundNo;
    private String reason;
    /** 原支付金额，单位分 */
    private Integer payPrice;
    /** 本次退款金额，单位分 */
    private Integer refundPrice;
    private String notifyUrl;

}
