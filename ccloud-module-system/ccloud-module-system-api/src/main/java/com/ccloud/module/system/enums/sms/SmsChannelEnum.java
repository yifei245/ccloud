package com.ccloud.module.system.enums.sms;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 短信渠道编码，与字典 system_sms_channel_code 保持一致
 */
@Getter
@AllArgsConstructor
public enum SmsChannelEnum {

    MOCK("MOCK", "模拟渠道（本地调试）"),
    ALIYUN("ALIYUN", "阿里云"),
    TENCENT("TENCENT", "腾讯云");

    private final String code;
    private final String name;

    public static SmsChannelEnum getByCode(String code) {
        return Arrays.stream(values()).filter(e -> e.code.equals(code)).findFirst().orElse(null);
    }
}
