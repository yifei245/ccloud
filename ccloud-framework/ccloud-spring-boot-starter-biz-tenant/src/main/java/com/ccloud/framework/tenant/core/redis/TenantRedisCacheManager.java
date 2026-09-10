package com.ccloud.framework.tenant.core.redis;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.redis.core.TimeoutRedisCacheManager;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

/**
 * 租户隔离的缓存管理器。
 *
 * <p>把租户编号拼进缓存名，使不同租户的同名缓存落在不同的 Redis Key 上。
 * 数据库做了行级隔离但缓存没隔离，是多租户系统里非常常见的漏洞——
 * 第一个租户查完把结果写进缓存，第二个租户直接命中缓存拿到了别人的数据。
 */
public class TenantRedisCacheManager extends TimeoutRedisCacheManager {

    public TenantRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }

    @Override
    public Cache getCache(String name) {
        Long tenantId = TenantContextHolder.getTenantId();
        // 忽略租户或无租户上下文时不加前缀，保持与全局缓存一致的行为
        if (tenantId != null && !TenantContextHolder.isIgnore()) {
            name = StrUtil.format("{}:{}", tenantId, name);
        }
        return super.getCache(name);
    }
}
