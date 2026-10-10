package com.ccloud.gateway.service.open;

/**
 * 开放平台服务不可用。与"应用不存在"严格区分：前者应返回 503 让调用方重试，
 * 误判为 401 会让第三方系统以为密钥失效而停止调用
 */
public class OpenAppUnavailableException extends RuntimeException {

    public OpenAppUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
