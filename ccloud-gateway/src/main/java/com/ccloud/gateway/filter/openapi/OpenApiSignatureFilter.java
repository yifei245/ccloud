package com.ccloud.gateway.filter.openapi;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.net.IpWhitelistUtils;
import com.ccloud.gateway.filter.security.InternalHeaderWriter;
import com.ccloud.gateway.filter.security.LoginUser;
import com.ccloud.gateway.filter.security.SecurityProperties;
import com.ccloud.gateway.service.open.OpenApp;
import com.ccloud.gateway.service.open.OpenAppService;
import com.ccloud.gateway.service.open.OpenAppUnavailableException;
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
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/**
 * 开放接口（{@code /open-api/**}）签名校验。请求头：
 * <ul>
 *   <li>{@code X-Open-App-Id}、{@code X-Open-Timestamp}（毫秒）、{@code X-Open-Nonce}、{@code X-Open-Sign}</li>
 *   <li>签名 = Base64(HmacSHA256(appSecret, METHOD + "\n" + 原始路径 + "\n" + 原始查询串 + "\n"
 *       + 时间戳 + "\n" + nonce + "\n" + 十六进制 SHA-256(请求体)))</li>
 * </ul>
 * 头名不复用 {@code X-Timestamp}：那是内部身份头，外部携带时会被剥离。
 * 请求体参与签名，否则截获请求的人可以改写金额等参数；nonce 存 Redis 防重放，多个网关实例共享。
 *
 * <p>应用由 open 服务管理，网关经 {@link OpenAppService} 读取（本地缓存）。校验顺序：
 * <ol>
 *   <li>应用不存在、已停用或已过期：401；open 不可用且无缓存：503；</li>
 *   <li>客户端 IP 不在应用白名单：403（IP 取连接对端，不信任 X-Forwarded-For）；</li>
 *   <li>时间戳、请求体大小、签名：401 / 413；</li>
 *   <li>nonce 重复：401；</li>
 *   <li>方法 + 原始路径未获授权：403。放在 nonce 之后，未授权的调用也消耗 nonce，防止同一请求被反复用来探测授权。</li>
 * </ol>
 * 通过后以开放应用身份（userType=3）写入带签名的内部身份头，业务服务只在 /open-api 下接受这种身份
 */
@Slf4j
@Component
public class OpenApiSignatureFilter implements GlobalFilter, Ordered {

    public static final int ORDER = -150;

    public static final String HEADER_APP_ID = "X-Open-App-Id";
    public static final String HEADER_TIMESTAMP = "X-Open-Timestamp";
    public static final String HEADER_NONCE = "X-Open-Nonce";
    public static final String HEADER_SIGN = "X-Open-Sign";

    /**
     * 验签通过的应用（{@link OpenApp}），供限流按应用计数
     */
    public static final String OPEN_APP_ATTRIBUTE = "ccloud.open_app";

    private static final String OPEN_API_PREFIX = "/open-api/";
    private static final String NONCE_KEY = "ccloud:open:nonce:%s:%s";

    private enum NonceState { FIRST_USE, DUPLICATE, UNAVAILABLE }

    /**
     * 应用查询结果：unavailable 为 true 表示 open 不可用（且本地没有缓存）
     */
    private record Lookup(OpenApp app, boolean unavailable) {
    }

    private final OpenApiProperties properties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final OpenAppService openAppService;
    private final SecurityProperties securityProperties;

    public OpenApiSignatureFilter(OpenApiProperties properties, ReactiveStringRedisTemplate redisTemplate,
                                  OpenAppService openAppService, SecurityProperties securityProperties) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.openAppService = openAppService;
        this.securityProperties = securityProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!request.getPath().pathWithinApplication().value().startsWith(OPEN_API_PREFIX)) {
            return chain.filter(exchange);
        }
        String appKey = request.getHeaders().getFirst(HEADER_APP_ID);
        String timestampText = request.getHeaders().getFirst(HEADER_TIMESTAMP);
        String nonce = request.getHeaders().getFirst(HEADER_NONCE);
        String sign = request.getHeaders().getFirst(HEADER_SIGN);
        if (isBlank(appKey) || isBlank(timestampText) || isBlank(nonce) || isBlank(sign) || nonce.length() > 64) {
            return reject(exchange, "缺少签名参数");
        }
        return openAppService.getApp(appKey)
                .map(app -> new Lookup(app, false))
                .defaultIfEmpty(new Lookup(null, false))
                // 只把"查询应用"本身的失败转成 503；verify 中的异常不经过这里，不会被吞成 503
                .onErrorResume(OpenAppUnavailableException.class, e -> {
                    log.error("[filter] 查询开放应用失败 appKey={}", appKey, e);
                    return Mono.just(new Lookup(null, true));
                })
                .flatMap(lookup -> lookup.unavailable()
                        ? WebFrameworkUtils.writeJSON(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                                CommonResult.error(HttpStatus.SERVICE_UNAVAILABLE.value(), "服务暂不可用，请稍后重试"))
                        : verify(exchange, chain, lookup.app(), appKey, timestampText, nonce, sign));
    }

    private Mono<Void> verify(ServerWebExchange exchange, GatewayFilterChain chain, OpenApp app, String appKey,
                              String timestampText, String nonce, String sign) {
        ServerHttpRequest request = exchange.getRequest();
        if (app == null || !app.isEnabled() || app.isExpired()) {
            return reject(exchange, "应用不存在、已停用或已过期");
        }
        // 客户端 IP 取连接的对端地址，不信任 X-Forwarded-For：后者可由客户端任意伪造
        InetSocketAddress remote = request.getRemoteAddress();
        String ip = remote != null && remote.getAddress() != null ? remote.getAddress().getHostAddress() : null;
        if (!IpWhitelistUtils.matches(app.getIpWhitelist(), ip)) {
            return forbidden(exchange, "IP 不在应用白名单内");
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
                    .setIfAbsent(String.format(NONCE_KEY, appKey, nonce), "1",
                            properties.getTimestampTolerance().multipliedBy(2))
                    .map(firstUse -> Boolean.TRUE.equals(firstUse) ? NonceState.FIRST_USE : NonceState.DUPLICATE)
                    .onErrorResume(e -> {
                        log.error("[filter] 校验 nonce 失败 appKey={}", appKey, e);
                        return Mono.just(NonceState.UNAVAILABLE);
                    })
                    .flatMap(state -> switch (state) {
                        case DUPLICATE -> reject(exchange, "请求重复");
                        case UNAVAILABLE -> WebFrameworkUtils.writeJSON(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                                CommonResult.error(HttpStatus.SERVICE_UNAVAILABLE.value(), "服务暂不可用，请稍后重试"));
                        case FIRST_USE -> {
                            // 按方法 + 原始路径精确匹配，不做规范化
                            if (!app.allows(request.getMethod().name(), request.getURI().getRawPath())) {
                                yield forbidden(exchange, "应用未获授权调用该接口");
                            }
                            LoginUser loginUser = new LoginUser();
                            loginUser.setId(app.getId());
                            loginUser.setUserType(UserTypeEnum.OPEN_APP.getValue());
                            loginUser.setTenantId(app.getTenantId());
                            loginUser.setInfo(Map.of("appKey", app.getAppKey()));
                            ServerHttpRequest forwarded = InternalHeaderWriter.write(
                                    cachedRequest.mutate().headers(h -> h.remove(HEADER_SIGN)).build(),
                                    loginUser, securityProperties.getInternalSignSecret());
                            ServerWebExchange verified = exchange.mutate().request(forwarded).build();
                            verified.getAttributes().put(OPEN_APP_ATTRIBUTE, app);
                            yield chain.filter(verified);
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

    private static Mono<Void> forbidden(ServerWebExchange exchange, String reason) {
        log.warn("[filter] 开放接口无权访问：{} path={}", reason, exchange.getRequest().getURI().getRawPath());
        return WebFrameworkUtils.writeJSON(exchange, HttpStatus.FORBIDDEN,
                CommonResult.error(GlobalErrorCodeConstants.FORBIDDEN.getCode(), "开放接口无权访问：" + reason));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
