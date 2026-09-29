package com.ccloud.module.system.framework.sms.core.property;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 短信渠道配置。equals 用于判断配置是否变化、客户端是否需要重建
 */
@Data
@Accessors(chain = true)
@ToString(exclude = "apiSecret")
public class SmsChannelProperties {

    private Long id;

    /**
     * 短信签名
     */
    private String signature;

    /**
     * 渠道编码，见 SmsChannelEnum
     */
    private String code;

    private String apiKey;

    private String apiSecret;

    private String callbackUrl;
}
