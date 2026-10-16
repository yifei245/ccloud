package com.ccloud.module.pay.framework.pay.core.client.dto;

/**
 * 渠道处理状态
 */
public enum PayClientStatus {

    /** 处理中或结果未知 */
    WAITING,
    SUCCESS,
    /** 渠道明确失败：支付失败或关闭、退款失败、转账失败 */
    FAILURE

}
