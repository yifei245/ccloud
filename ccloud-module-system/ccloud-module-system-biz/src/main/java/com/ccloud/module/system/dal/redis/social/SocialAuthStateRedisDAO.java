package com.ccloud.module.system.dal.redis.social;

import jakarta.annotation.Resource;
import me.zhyd.oauth.cache.AuthStateCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 社交授权的 state 缓存。JustAuth 回调时校验 state 以防 CSRF；默认实现存在本机内存，
 * 多实例部署时回调落到另一节点会校验失败，因此放 Redis
 */
@Repository
public class SocialAuthStateRedisDAO implements AuthStateCache {

    private static final String KEY_FORMAT = "ccloud:social_auth_state:%s";
    /**
     * 与 JustAuth 默认一致：用户须在 3 分钟内完成授权
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(3);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void cache(String key, String value) {
        cache(key, value, DEFAULT_TIMEOUT.toMillis());
    }

    @Override
    public void cache(String key, String value, long timeout) {
        stringRedisTemplate.opsForValue().set(String.format(KEY_FORMAT, key), value, Duration.ofMillis(timeout));
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(String.format(KEY_FORMAT, key));
    }

    @Override
    public boolean containsKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(String.format(KEY_FORMAT, key)));
    }
}
