package com.ccloud.module.member.service.auth;

/**
 * 登录请求的来源信息，由 Controller 从请求头中提取
 *
 * @param userIp    客户端 IP
 * @param userAgent 浏览器 UA
 * @param terminal  终端，见 {@link com.ccloud.framework.common.enums.TerminalEnum}，未知时为 null
 * @param clientId  OAuth2 客户端编号，同一用户在不同客户端的登录互不影响
 */
public record MemberLoginContext(String userIp, String userAgent, Integer terminal, String clientId) {
}
