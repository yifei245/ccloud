package com.ccloud.module.member.dal.redis.auth;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 会员密码登录失败次数，按租户 + 手机号计数，用于锁定暴力破解。
 * key 与管理员的计数分开：两套用户体系的账号可能同名
 */
@Repository
public class MemberLoginFailCountRedisDAO {

    private static final String KEY_FORMAT = "ccloud:member:login_fail_count:%s:%s";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static String formatKey(Long tenantId, String mobile) {
        return String.format(KEY_FORMAT, tenantId, mobile);
    }

    public int get(Long tenantId, String mobile) {
        String value = stringRedisTemplate.opsForValue().get(formatKey(tenantId, mobile));
        return value != null ? Integer.parseInt(value) : 0;
    }

    public void increment(Long tenantId, String mobile, Duration duration) {
        String key = formatKey(tenantId, mobile);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, duration);
        }
    }

    public void delete(Long tenantId, String mobile) {
        stringRedisTemplate.delete(formatKey(tenantId, mobile));
    }
}
