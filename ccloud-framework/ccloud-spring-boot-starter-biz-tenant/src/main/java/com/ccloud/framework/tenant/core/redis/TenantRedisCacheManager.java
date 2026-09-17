package com.ccloud.framework.tenant.core.redis;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.redis.core.TimeoutRedisCacheManager;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.Collections;
import java.util.Set;

/**
 * 租户隔离的缓存管理器。
 *
 * <p>把租户编号拼进缓存名，使不同租户的同名缓存落在不同的 Redis Key 上。
 * 数据库做了行级隔离但缓存没隔离，是多租户系统里非常常见的漏洞——
 * 第一个租户查完把结果写进缓存，第二个租户直接命中缓存拿到了别人的数据。
 *
 * <p>反过来，全局表（菜单、字典等不含 tenant_id 的表）的缓存必须在 {@code ignoreCaches} 中声明：
 * 若也按租户隔离，系统租户修改数据后只能清掉自己那一份，其他租户会持续读到旧数据。
 */
public class TenantRedisCacheManager extends TimeoutRedisCacheManager {

    private static final String TTL_SPLIT = "#";

    /**
     * 全局共享的缓存名，不含 "#ttl" 后缀
     */
    private final Set<String> ignoreCaches;

    public TenantRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        this(cacheWriter, defaultCacheConfiguration, Collections.emptySet());
    }

    public TenantRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration,
                                   Set<String> ignoreCaches) {
        super(cacheWriter, defaultCacheConfiguration);
        this.ignoreCaches = ignoreCaches == null ? Collections.emptySet() : Set.copyOf(ignoreCaches);
    }

    @Override
    public Cache getCache(String name) {
        Long tenantId = TenantContextHolder.getTenantId();
        // 忽略租户或无租户上下文时不加前缀，保持与全局缓存一致的行为
        if (tenantId != null && !TenantContextHolder.isIgnore() && !isIgnoreCache(name)) {
            name = StrUtil.format("{}:{}", tenantId, name);
        }
        return super.getCache(name);
    }

    private boolean isIgnoreCache(String name) {
        int index = name.lastIndexOf(TTL_SPLIT);
        return ignoreCaches.contains(index == -1 ? name : name.substring(0, index));
    }
}
