package com.ccloud.gateway.filter.security;

import com.ccloud.framework.common.util.json.JsonUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 按内部身份头协议写入下游请求：
 * <ul>
 *   <li>{@code X-Login-User}：Base64(UTF8(JSON(LoginUser)))</li>
 *   <li>{@code X-Login-User-Sign}：Base64(HmacSHA256(secret, X-Login-User + "." + X-Timestamp))</li>
 *   <li>{@code X-Timestamp}：毫秒时间戳，业务侧校验时效</li>
 *   <li>{@code X-Tenant-Id}：令牌所属租户，覆盖请求自带的值</li>
 * </ul>
 * 与业务侧 LoginUserFilter、InternalSignUtils 必须逐字一致
 */
public final class InternalHeaderWriter {

    public static final String HEADER_LOGIN_USER = "X-Login-User";
    public static final String HEADER_LOGIN_USER_SIGN = "X-Login-User-Sign";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /**
     * 只能由网关写入的头，外部请求携带时一律剥离
     */
    public static final List<String> INTERNAL_HEADERS = List.of(HEADER_LOGIN_USER, HEADER_LOGIN_USER_SIGN,
            HEADER_TIMESTAMP);

    private InternalHeaderWriter() {
    }

    public static ServerHttpRequest write(ServerHttpRequest request, LoginUser loginUser, String secret) {
        String payload = Base64.getEncoder().encodeToString(
                JsonUtils.toJsonString(loginUser).getBytes(StandardCharsets.UTF_8));
        long timestamp = System.currentTimeMillis();
        String sign = sign(payload, timestamp, secret);
        return request.mutate().headers(headers -> {
            headers.set(HEADER_LOGIN_USER, payload);
            headers.set(HEADER_LOGIN_USER_SIGN, sign);
            headers.set(HEADER_TIMESTAMP, String.valueOf(timestamp));
            if (loginUser.getTenantId() != null) {
                headers.set(HEADER_TENANT_ID, String.valueOf(loginUser.getTenantId()));
            }
        }).build();
    }

    static String sign(String payload, long timestamp, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal((payload + "." + timestamp).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("内部身份签名失败", e);
        }
    }
}
