package com.ccloud.framework.signature.core.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * nonce 去重。
 *
 * <p>签名本身只能证明"报文没被篡改"，证明不了"这不是一次重放"。
 * 把用过的 nonce 记下来并拒绝重复值，才能堵住抓包重放。
 */
@RequiredArgsConstructor
public class ApiSignatureRedisDAO {

    private static final String NONCE_KEY = "ccloud:open:nonce:%s:%s";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 原子地占用一个 nonce
     *
     * @return true 表示首次使用；false 表示该 nonce 已被用过，应拒绝
     */
    public boolean tryAcquireNonce(String appKey, String nonce, Duration timeout) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(String.format(NONCE_KEY, appKey, nonce), "1", timeout);
        return Boolean.TRUE.equals(success);
    }
}
