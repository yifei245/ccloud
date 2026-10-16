package com.ccloud.module.open.enums.notify;

/**
 * 回调事件类型。业务模块新增事件时在此登记
 */
public interface OpenNotifyEvents {

    /**
     * 测试回调，由管理端"发送测试回调"触发，data 为 {"message":"ccloud test"}
     */
    String TEST = "open.test";

    /**
     * 支付成功，由 pay 发送，data 为 merchantOrderId、orderNo、price、status、successTime、channelCode
     */
    String PAY_ORDER_PAID = "pay.order.paid";
    /**
     * 退款成功
     */
    String PAY_REFUND_SUCCESS = "pay.refund.success";
    /**
     * 退款失败
     */
    String PAY_REFUND_FAILURE = "pay.refund.failure";
    /**
     * 转账成功
     */
    String PAY_TRANSFER_SUCCESS = "pay.transfer.success";
    /**
     * 转账失败
     */
    String PAY_TRANSFER_FAILURE = "pay.transfer.failure";
}
