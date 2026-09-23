package com.ccloud.module.system.framework.sms.core.client;

import com.ccloud.module.system.framework.sms.core.property.SmsChannelProperties;

/**
 * 短信客户端工厂：按渠道编号缓存客户端，渠道配置变化时重建
 */
public interface SmsClientFactory {

    /**
     * @return 已创建的客户端，未创建过返回 null
     */
    SmsClient getSmsClient(Long channelId);

    /**
     * 配置未变时返回已缓存的客户端，否则按新配置创建或刷新
     */
    SmsClient createOrUpdateSmsClient(SmsChannelProperties properties);
}
