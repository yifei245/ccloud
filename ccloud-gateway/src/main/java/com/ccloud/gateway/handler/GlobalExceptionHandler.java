package com.ccloud.gateway.handler;

import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.gateway.util.WebFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关自身产生的异常（无路由、下游无实例、连接超时等）统一转为 {@link CommonResult} JSON，
 * 而不是 Spring 默认的 HTML 错误页或 whitelabel JSON。HTTP 状态码与 code 保持一致
 */
@Slf4j
@Component
@Order(-1) // 先于 Spring Boot 默认的 DefaultErrorWebExceptionHandler（-1 之后）
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        if (ex instanceof ResponseStatusException statusException) {
            HttpStatusCode status = statusException.getStatusCode();
            log.debug("[handle] {} {} -> {}", exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getRawPath(), status);
            return WebFrameworkUtils.writeJSON(exchange, status, CommonResult.error(status.value(), message(status)));
        }
        log.error("[handle] {} {} 网关异常", exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getRawPath(), ex);
        return WebFrameworkUtils.writeJSON(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
                CommonResult.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR));
    }

    /**
     * 不回显异常原文：其中可能带有下游服务名、内网地址
     */
    private static String message(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> GlobalErrorCodeConstants.NOT_FOUND.getMsg();
            case 405 -> GlobalErrorCodeConstants.METHOD_NOT_ALLOWED.getMsg();
            case 503 -> "服务暂不可用，请稍后重试";
            case 504 -> "服务响应超时，请稍后重试";
            default -> status.is4xxClientError() ? GlobalErrorCodeConstants.BAD_REQUEST.getMsg()
                    : GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getMsg();
        };
    }
}
