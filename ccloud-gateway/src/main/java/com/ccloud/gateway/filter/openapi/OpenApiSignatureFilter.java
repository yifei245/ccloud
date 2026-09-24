package com.ccloud.gateway.filter.openapi;

import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.gateway.filter.security.InternalHeaderWriter;
import com.ccloud.gateway.util.WebFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 开放接口（{@code /open-api/**}）签名校验。请求头：
 * <ul>
 *   <li>{@code X-Open-App-Id}、{@code X-Open-Timestamp}（毫秒）、{@code X-Open-Nonce}、{@code X-Open-Sign}</li>
 *   <li>签名 = Base64(HmacSHA256(appSecret, METHOD + "\n" + 原始路径 + "\n" + 原始查询串 + "\n"
 *       + 时间戳 + "\n" + nonce + "\n" + 十六进制 SHA-256(请求体)))</li>
 * </ul>
 * 头名不复用 {@code X-Timestamp}：那是内部身份头，外部携带时会被剥离。
 * 请求体参与签名，否则截获请求的人可以改写金额等参数；nonce 存 Redis 防重放，多个网关实例共享
 */
@Slf4j
@Component
public class OpenApiSignatureFilter implements GlobalFilter, Ordered {

    public static final int ORDER = -150;

    public static final String HEADER_APP_ID = "X-Open-App-Id";
    public static final String HEADER_TIMESTAMP = "X-Open-Timestamp";
    public static final String HEADER_NONCE = "X-Open-Nonce";
    public static final String HEADER_SIGN = "X-Open-Sign";

    private static final String OPEN_API_PREFIX = "/open-api/";
    private static final String NONCE_KEY = "ccloud:open:nonce:%s:%s";

    private enum NonceState { FIRST_USE, DUPLICATE, UNAVAILABLE }

    private final OpenApiProperties properties;
    private final ReactiveStringRedisTemplate redisTemplate;

    public OpenApiSignatureFilter(OpenApiProperties properties, ReactiveStringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!request.getPath().pathWithinApplication().value().startsWith(OPEN_API_PREFIX)) {
            return chain.filter(exchange);
        }
        String appId = request.getHeaders().getFirst(HEADER_APP_ID);
        String timestampText = request.getHeaders().getFirst(HEADER_TIMESTAMP);
        String nonce = request.getHeaders().getFirst(HEADER_NONCE);
        String sign = request.getHeaders().getFirst(HEADER_SIGN);
        if (isBlank(appId) || isBlank(timestampText) || isBlank(nonce) || isBlank(sign) || nonce.length() > 64) {
            return reject(exchange, "缺少签名参数");
        }
        OpenApiProperties.App app = properties.getApps().stream()
                .filter(a -> a.isEnabled() && Objects.equals(a.getAppId(), appId)).findFirst().orElse(null);
        if (app == null) {
            return reject(exchange, "应用不存在或已停用");
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampText);
        } catch (NumberFormatException e) {
            return reject(exchange, "时间戳不合法");
        }
        if (Math.abs(System.currentTimeMillis() - timestamp) > properties.getTimestampTolerance().toMillis()) {
            return reject(exchange, "请求已过期");
        }
        long contentLength = request.getHeaders().getContentLength();
        if (contentLength > properties.getMaxBodySize()) {
            return WebFrameworkUtils.writeJSON(exchange, HttpStatus.PAYLOAD_TOO_LARGE,
                    CommonResult.error(HttpStatus.PAYLOAD_TOO_LARGE.value(), "请求体过大"));
        }

        // 缓存请求体：签名要读取它，下游还要再读一次
        return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange, cachedRequest -> {
            DataBuffer body = exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
            byte[] bytes = body == null ? new byte[0] : readBytes(body);
            String expected = sign(app.getAppSecret(), request.getMethod().name(), request.getURI().getRawPath(),
                    request.getURI().getRawQuery(), timestamp, nonce, bytes);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), sign.getBytes(StandardCharsets.UTF_8))) {
                return reject(exchange, "签名不正确");
            }
            // 验签通过后再占用 nonce，避免伪造请求消耗合法调用方的 nonce
            return redisTemplate.opsForValue()
                    .setIfAbsent(String.format(NONCE_KEY, appId, nonce), "1",
                            properties.getTimestampTolerance().multipliedBy(2))
                    .map(firstUse -> Boolean.TRUE.equals(firstUse) ? NonceState.FIRST_USE : NonceState.DUPLICATE)
                    .onErrorResume(e -> {
                        log.error("[filter] 校验 nonce 失败 appId={}", appId, e);
                        return Mono.just(NonceState.UNAVAILABLE);
                    })
                    .flatMap(state -> switch (state) {
                        case DUPLICATE -> reject(exchange, "请求重复");
                        case UNAVAILABLE -> WebFrameworkUtils.writeJSON(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                                CommonResult.error(HttpStatus.SERVICE_UNAVAILABLE.value(), "服务暂不可用，请稍后重试"));
                        case FIRST_USE -> {
                            ServerHttpRequest forwarded = cachedRequest.mutate().headers(headers -> {
                                headers.remove(HEADER_SIGN);
                                if (app.getTenantId() != null) {
                                    headers.set(InternalHeaderWriter.HEADER_TENANT_ID, String.valueOf(app.getTenantId()));
                                }
                            }).build();
                            yield chain.filter(exchange.mutate().request(forwarded).build());
                        }
                    });
        });
    }

    static String sign(String secret, String method, String rawPath, String rawQuery, long timestamp, String nonce,
                       byte[] body) {
        try {
            String bodyHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
            String stringToSign = method + "\n" + rawPath + "\n" + (rawQuery == null ? "" : rawQuery) + "\n"
                    + timestamp + "\n" + nonce + "\n" + bodyHash;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("开放接口签名计算失败", e);
        }
    }

    private static byte[] readBytes(DataBuffer buffer) {
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.toByteBuffer(buffer.readPosition(), java.nio.ByteBuffer.wrap(bytes), 0, bytes.length);
        return bytes;
    }

    private static Mono<Void> reject(ServerWebExchange exchange, String reason) {
        log.warn("[filter] 开放接口签名校验失败：{} path={}", reason, exchange.getRequest().getURI().getRawPath());
        return WebFrameworkUtils.writeJSON(exchange, HttpStatus.UNAUTHORIZED,
                CommonResult.error(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "开放接口认证失败：" + reason));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
