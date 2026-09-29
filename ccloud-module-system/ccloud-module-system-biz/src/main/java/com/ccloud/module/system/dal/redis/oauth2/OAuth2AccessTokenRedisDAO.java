package com.ccloud.module.system.dal.redis.oauth2;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 访问令牌的 Redis 缓存。每个请求都要校验令牌，先查 Redis 以免压垮数据库。
 * 过期时间与令牌本身一致，令牌过期时缓存同时失效
 */
@Repository
public class OAuth2AccessTokenRedisDAO {

    private static final String KEY_FORMAT = "ccloud:oauth2_access_token:%s";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public static String formatKey(String accessToken) {
        return String.format(KEY_FORMAT, accessToken);
    }

    public OAuth2AccessTokenDO get(String accessToken) {
        String value = stringRedisTemplate.opsForValue().get(formatKey(accessToken));
        return value != null ? JsonUtils.parseObject(value, OAuth2AccessTokenDO.class) : null;
    }

    public void set(OAuth2AccessTokenDO accessToken) {
        long seconds = Duration.between(LocalDateTime.now(), accessToken.getExpiresTime()).getSeconds();
        if (seconds <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(formatKey(accessToken.getAccessToken()),
                JsonUtils.toJsonString(accessToken), Duration.ofSeconds(seconds));
    }

    public void delete(String accessToken) {
        stringRedisTemplate.delete(formatKey(accessToken));
    }

    public void deleteList(Collection<String> accessTokens) {
        stringRedisTemplate.delete(accessTokens.stream().map(OAuth2AccessTokenRedisDAO::formatKey).toList());
    }
}
