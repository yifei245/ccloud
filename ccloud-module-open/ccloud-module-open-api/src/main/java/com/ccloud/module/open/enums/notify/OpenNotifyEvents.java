package com.ccloud.module.open.enums.notify;

/**
 * 回调事件类型。业务模块新增事件时在此登记
 */
public interface OpenNotifyEvents {

    /**
     * 测试回调，由管理端"发送测试回调"触发，data 为 {"message":"ccloud test"}
     */
    String TEST = "open.test";
}
