package com.ccloud.module.pay.service.notify;

import java.util.Map;

/**
 * 把支付、退款、转账结果经开放平台回调给外部系统
 */
public interface PayOpenNotifyService {

    /**
     * 转发一个事件。支付应用不存在或未绑定开放应用时什么也不做；开放平台调用失败（返回失败码或抛异常）时向上抛出，由调用方让消息重投
     *
     * @param messageId 消息编号，作为事件编号：重投时开放平台据此去重
     * @param event     事件类型，见 OpenNotifyEvents
     * @param appId     支付应用编号
     * @param data      事件数据
     */
    void forward(String messageId, String event, Long appId, Map<String, Object> data);
}
