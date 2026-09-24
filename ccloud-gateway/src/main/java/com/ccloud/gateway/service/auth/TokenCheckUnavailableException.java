package com.ccloud.gateway.service.auth;

/**
 * 令牌校验服务不可用。与"令牌无效"严格区分：前者应返回 503 让客户端重试，
 * 误判为 401 会让前端清掉登录态、把所有用户踢回登录页
 */
public class TokenCheckUnavailableException extends RuntimeException {

    public TokenCheckUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
