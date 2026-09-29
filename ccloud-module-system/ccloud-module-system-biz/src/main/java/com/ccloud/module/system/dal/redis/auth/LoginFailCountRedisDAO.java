package com.ccloud.module.system.dal.redis.auth;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 登录失败次数，按租户与账号分别计数。
 *
 * <p>计数在首次失败时设置过期时间，因此"锁定"是滑动的：锁定期内不再累加，到期自动解锁
 */
@Repository
public class LoginFailCountRedisDAO {

    private static final String KEY_FORMAT = "ccloud:login_fail_count:%s:%s";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static String formatKey(Long tenantId, String username) {
        return String.format(KEY_FORMAT, tenantId, username);
    }

    public int get(Long tenantId, String username) {
        String value = stringRedisTemplate.opsForValue().get(formatKey(tenantId, username));
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * @return 自增后的失败次数
     */
    public int increment(Long tenantId, String username, Duration duration) {
        String key = formatKey(tenantId, username);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, duration);
        }
        return count != null ? count.intValue() : 1;
    }

    public void delete(Long tenantId, String username) {
        stringRedisTemplate.delete(formatKey(tenantId, username));
    }
}
