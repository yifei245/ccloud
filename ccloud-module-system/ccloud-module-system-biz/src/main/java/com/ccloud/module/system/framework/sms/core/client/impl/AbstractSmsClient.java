package com.ccloud.module.system.framework.sms.core.client.impl;

import com.ccloud.module.system.framework.sms.core.client.SmsClient;
import com.ccloud.module.system.framework.sms.core.property.SmsChannelProperties;
import lombok.Getter;

/**
 * 短信客户端基类：持有渠道配置，配置变化时重新初始化渠道 SDK
 */
public abstract class AbstractSmsClient implements SmsClient {

    @Getter
    protected volatile SmsChannelProperties properties;

    protected AbstractSmsClient(SmsChannelProperties properties) {
        this.properties = properties;
        doInit();
    }

    @Override
    public Long getId() {
        return properties.getId();
    }

    /**
     * 按当前配置初始化渠道 SDK
     */
    protected abstract void doInit();

    /**
     * 配置变化时刷新
     */
    public synchronized void refresh(SmsChannelProperties newProperties) {
        if (newProperties.equals(properties)) {
            return;
        }
        this.properties = newProperties;
        doInit();
    }
}
