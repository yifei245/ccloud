package com.ccloud.module.system.dal.redis.auth;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 图形验证码。校验一次即删除，无论成功与否，避免被暴力尝试
 */
@Repository
public class CaptchaRedisDAO {

    private static final String KEY_FORMAT = "ccloud:captcha_code:%s";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static String formatKey(String uuid) {
        return String.format(KEY_FORMAT, uuid);
    }

    public void set(String uuid, String code, Duration timeout) {
        stringRedisTemplate.opsForValue().set(formatKey(uuid), code, timeout);
    }

    public String get(String uuid) {
        return stringRedisTemplate.opsForValue().get(formatKey(uuid));
    }

    /**
     * @return 取出并删除，不存在返回 null
     */
    public String getAndDelete(String uuid) {
        return stringRedisTemplate.opsForValue().getAndDelete(formatKey(uuid));
    }
}
