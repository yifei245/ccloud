package com.ccloud.framework.redis.core;

import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 支持在缓存名里直接声明 TTL 的缓存管理器。
 *
 * <p>用法：{@code @Cacheable(value = "user#30m", key = "#id")}，井号后是过期时间。
 * 这样每个缓存的有效期跟着注解走，不必为每个缓存在配置文件里单独登记一条，
 * 代码阅读时也能一眼看出这份缓存能存多久。
 *
 * <p>后缀非法或缺失时回退到默认配置的 TTL，不抛异常——缓存配置写错
 * 不应该让应用起不来。
 */
public class TimeoutRedisCacheManager extends RedisCacheManager {

    private static final String SPLIT = "#";

    /**
     * 匹配 30s / 5m / 2h / 1d 形式的后缀
     */
    private static final Pattern TTL_PATTERN = Pattern.compile("^(\\d+)([smhd])$");

    public TimeoutRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }

    /**
     * 放宽为 public：父类声明为 protected，但该方法是本类行为的入口，
     * 需要能被直接验证。
     */
    @Override
    public RedisCache getMissingCache(String name) {
        return super.getMissingCache(name);
    }

    @Override
    protected RedisCache createRedisCache(String name, @Nullable RedisCacheConfiguration cacheConfig) {
        int index = name.lastIndexOf(SPLIT);
        if (index == -1 || cacheConfig == null) {
            return super.createRedisCache(name, cacheConfig);
        }
        Duration ttl = parseTtl(name.substring(index + 1));
        if (ttl == null) {
            return super.createRedisCache(name, cacheConfig);
        }
        return super.createRedisCache(name, cacheConfig.entryTtl(ttl));
    }

    /**
     * @return 解析出的过期时间；格式非法时返回 null
     */
    private static Duration parseTtl(String suffix) {
        Matcher matcher = TTL_PATTERN.matcher(suffix);
        if (!matcher.matches()) {
            return null;
        }
        long value = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "s" -> Duration.ofSeconds(value);
            case "m" -> Duration.ofMinutes(value);
            case "h" -> Duration.ofHours(value);
            case "d" -> Duration.ofDays(value);
            default -> null;
        };
    }
}
