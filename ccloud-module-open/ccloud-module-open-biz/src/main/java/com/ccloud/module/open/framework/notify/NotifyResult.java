package com.ccloud.module.open.framework.notify;

/**
 * 一次回调发送的结果
 *
 * @param success      对方是否返回 2xx
 * @param httpStatus   对方的 HTTP 状态码；请求未发出（地址受限、连接失败、超时）时为 0
 * @param responseBody 对方响应（截断到 2048 字符）或失败原因
 */
public record NotifyResult(boolean success, int httpStatus, String responseBody) {
}
