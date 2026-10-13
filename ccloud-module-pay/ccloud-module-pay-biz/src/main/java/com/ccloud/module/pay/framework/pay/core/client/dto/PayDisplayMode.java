package com.ccloud.module.pay.framework.pay.core.client.dto;

/**
 * 等待用户支付时的展示方式
 */
public enum PayDisplayMode {

    /** 二维码，displayContent 为二维码内容 */
    QR_CODE,
    /** 跳转地址，displayContent 为 URL */
    URL,
    /** 自动提交的 HTML 表单 */
    FORM,
    /** 唤起 App，displayContent 为 App 的参数 */
    APP,
    NONE

}
