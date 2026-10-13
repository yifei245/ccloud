package com.ccloud.module.pay.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 支付渠道编码，字典 pay_channel_code
 */
@Getter
@AllArgsConstructor
public enum PayChannelEnum {

    MOCK("mock", "模拟支付", true),
    WALLET("wallet", "钱包支付", true),
    WX_LITE("wx_lite", "微信小程序支付", false),
    WX_PUB("wx_pub", "微信公众号支付", false),
    WX_APP("wx_app", "微信 App 支付", false),
    WX_NATIVE("wx_native", "微信扫码支付", false),
    ALIPAY_PC("alipay_pc", "支付宝 PC 网站", false),
    ALIPAY_WAP("alipay_wap", "支付宝 Wap 网站", false),
    ALIPAY_APP("alipay_app", "支付宝 App", false),
    ALIPAY_QR("alipay_qr", "支付宝扫码", false);

    public static final String DICT_TYPE = "pay_channel_code";

    private final String code;
    private final String name;
    /** 本期是否有渠道实现。未实现的编码保留在字典中，新建渠道时提示未实现 */
    private final boolean implemented;

    public static PayChannelEnum getByCode(String code) {
        return Arrays.stream(values()).filter(e -> e.code.equals(code)).findFirst().orElse(null);
    }
}
