package com.ccloud.framework.redis.core.util;

/**
 * Redis Key 工具
 */
public class RedisKeyUtils {

    /**
     * 全局 Key 前缀。所有 Key 都带该前缀，便于与其他共用 Redis 实例的应用区分，
     * 也方便按前缀批量清理。
     */
    public static final String PREFIX = "ccloud:";

    public static String build(String... segments) {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(segments[i]);
        }
        return sb.toString();
    }

    /**
     * 构造租户隔离的 Key。多租户下同名缓存必须按租户分开，
     * 否则 A 租户能读到 B 租户的数据。
     */
    public static String buildWithTenant(Long tenantId, String... segments) {
        return build(segments) + ":t" + tenantId;
    }
}
