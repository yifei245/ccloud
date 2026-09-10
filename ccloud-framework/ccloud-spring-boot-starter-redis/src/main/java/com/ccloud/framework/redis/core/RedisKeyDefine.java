package com.ccloud.framework.redis.core;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;

/**
 * Redis Key 定义。
 *
 * <p>要求每个 Key 都显式登记用途、类型与过期时间。散落在各处的字符串拼接
 * 会让人无法回答"这个 key 是谁写的、什么时候过期"，出问题时排查成本极高。
 */
@Data
@AllArgsConstructor
public class RedisKeyDefine {

    public enum KeyTypeEnum {
        STRING, LIST, SET, ZSET, HASH, STREAM, PUBSUB
    }

    /**
     * Key 模板，例如 {@code ccloud:oauth2_access_token:%s}
     */
    private final String keyTemplate;

    private final KeyTypeEnum keyType;

    private final Class<?> valueType;

    /**
     * 过期时间；{@link Duration#ZERO} 表示不过期
     */
    private final Duration timeout;

    private final String memo;

    public String format(Object... args) {
        return String.format(keyTemplate, args);
    }
}
