package com.ccloud.framework.signature.core;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 开放 API 的签名计算与校验。
 *
 * <p>签名串 = 按键名字典序拼接的非空查询参数 + 请求体 + 时间戳 + nonce，
 * 再以 appSecret 做 HMAC-SHA256。
 *
 * <p>三个设计要点：
 * <ul>
 *   <li><b>必须覆盖请求体</b>——只签查询参数的话，把 POST body 里的金额改掉不会被察觉；</li>
 *   <li><b>参数按字典序排列</b>——调用方的 Map 遍历顺序不可控，不排序会随机验签失败；</li>
 *   <li><b>空值参数不参与</b>——各语言 SDK 对"空字符串字段要不要发"的处理不一致，
 *       把它们纳入签名会造成难以排查的偶发失败。</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class ApiSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * 允许的时钟超前量，应对多机时钟漂移
     */
    private static final Duration CLOCK_SKEW_AHEAD = Duration.ofMinutes(1);

    private final Duration tolerance;

    public String sign(Map<String, String> params, String body, long timestamp, String nonce, String secret) {
        String content = buildContent(params, body, timestamp, nonce);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("API 签名计算失败", e);
        }
    }

    /**
     * 校验签名。任何异常都归为失败，不向调用方抛出——
     * 伪造输入导致 500 本身就是一种信息泄漏。
     */
    public boolean verify(Map<String, String> params, String body, long timestamp,
                          String nonce, String sign, String secret) {
        if (StrUtil.isBlank(sign) || StrUtil.isBlank(secret)) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (timestamp < now - tolerance.toMillis() || timestamp > now + CLOCK_SKEW_AHEAD.toMillis()) {
            return false;
        }
        try {
            String expected = sign(params, body, timestamp, nonce, secret);
            // 定长比较，避免通过响应时间差逐字节猜出签名
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    sign.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("[verify] 签名校验异常，判为失败", e);
            return false;
        }
    }

    private String buildContent(Map<String, String> params, String body, long timestamp, String nonce) {
        StringBuilder sb = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            // TreeMap 保证字典序；空值跳过
            new TreeMap<>(params).forEach((key, value) -> {
                if (StrUtil.isNotBlank(value)) {
                    sb.append(key).append('=').append(value).append('&');
                }
            });
            if (!sb.isEmpty()) {
                sb.setLength(sb.length() - 1);
            }
        }
        return sb.append(StrUtil.nullToEmpty(body))
                .append(timestamp)
                .append(StrUtil.nullToEmpty(nonce))
                .toString();
    }
}
