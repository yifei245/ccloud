package com.ccloud.module.system.dal.redis.sms;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 短信验证码输错次数，按验证码记录编号计数：重新获取验证码即换新计数
 */
@Repository
public class SmsCodeFailCountRedisDAO {

    private static final String KEY_FORMAT = "ccloud:sms_code_fail_count:%d";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public int get(Long codeId) {
        String value = stringRedisTemplate.opsForValue().get(String.format(KEY_FORMAT, codeId));
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * @param timeout 取验证码有效期，过期后计数随之失效
     */
    public void increment(Long codeId, Duration timeout) {
        String key = String.format(KEY_FORMAT, codeId);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, timeout);
        }
    }
}
