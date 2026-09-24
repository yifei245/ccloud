package com.ccloud.gateway.util;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关（WebFlux）侧的 Web 工具。框架的 WebFrameworkUtils 基于 Servlet，网关不能使用
 */
public final class WebFrameworkUtils {

    private WebFrameworkUtils() {
    }

    /**
     * 以 JSON 写出 {@link CommonResult}。网关自己产生的错误与下游服务保持同一响应格式，前端只需一套处理逻辑
     */
    public static Mono<Void> writeJSON(ServerWebExchange exchange, HttpStatusCode status, CommonResult<?> result) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = JsonUtils.toJsonString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
