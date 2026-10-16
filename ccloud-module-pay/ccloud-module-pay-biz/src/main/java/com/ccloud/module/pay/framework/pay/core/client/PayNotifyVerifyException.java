package com.ccloud.module.pay.framework.pay.core.client;

/**
 * 渠道回调验签或解析失败，回调入口据此返回 400
 */
public class PayNotifyVerifyException extends RuntimeException {

    public PayNotifyVerifyException(String message) {
        super(message);
    }

}
