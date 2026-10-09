package com.ccloud.module.infra.dal.redis;

/**
 * infra 的 Redis 缓存名。全局数据的缓存须登记在 ccloud.tenant.ignore-caches 中（不带 #ttl 后缀）
 */
public interface RedisKeyConstants {

    /**
     * 参数配置：key 为参数键名，value 为 ConfigDO。参数为全局数据
     */
    String CONFIG = "infra_config#1h";
}
