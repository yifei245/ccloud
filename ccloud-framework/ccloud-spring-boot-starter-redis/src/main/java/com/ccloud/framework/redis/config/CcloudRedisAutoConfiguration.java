package com.ccloud.framework.redis.config;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 自动配置。
 *
 * <p>序列化规则：Key 用纯字符串（便于在 redis-cli 里直接查看），
 * Value 用 JSON（可读、跨语言、不受类结构变更影响，好过 JDK 序列化）。
 */
@AutoConfiguration
public class CcloudRedisAutoConfiguration {

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(buildRedisSerializer());
        template.setHashValueSerializer(buildRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 与 {@link JsonUtils} 共用同一套 Jackson 配置（时间格式等），
     * 但 Long 保持数字：缓存由 Java 读回，写成字符串会让单值 Long 还原成 String。
     *
     * <p>对所有值（含 Long 等 final 类型）记录类型信息：NON_FINAL 不记录 Long，
     * 小数值会被还原成 Integer，缓存方法返回时抛 ClassCastException；集合与对象同样依赖类型信息还原。
     */
    public static RedisSerializer<Object> buildRedisSerializer() {
        ObjectMapper objectMapper = JsonUtils.createObjectMapper(false);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    public StringRedisSerializer stringRedisSerializer() {
        return new StringRedisSerializer();
    }
}
