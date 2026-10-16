package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Redis 中保存的模拟单据
 */
@Data
@Accessors(chain = true)
public class MockPayState {

    /** order / refund / transfer */
    private String type;
    /** 外部单号 */
    private String no;
    /** 退款时对应的原支付外部单号 */
    private String outTradeNo;
    private Integer price;
    private String notifyUrl;
    /** WAITING / SUCCESS / FAILURE */
    private String status;
    private Long successTime;

}
