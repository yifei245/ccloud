package com.ccloud.module.pay.framework.pay.core.client;

/**
 * 渠道实现的入口。每个渠道实现一个，声明为 Spring Bean 即注册到 {@link PayClientFactory}
 */
public interface PayClientProvider {

    /** 渠道编码，与字典 pay_channel_code 一致 */
    String getCode();

    Class<? extends PayClientConfig> getConfigClass();

    PayClient create(Long channelId, PayClientConfig config);

}
