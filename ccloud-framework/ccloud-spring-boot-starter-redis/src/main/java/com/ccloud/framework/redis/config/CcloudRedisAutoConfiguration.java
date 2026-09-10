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
     * 与 {@link JsonUtils} 共用同一套 Jackson 配置，
     * 保证同一个对象在 HTTP 响应与 Redis 里的表示一致。
     *
     * <p>额外开启类型信息：反序列化时需要知道具体类型，
     * 否则集合类字段会退化成 LinkedHashMap。
     */
    public static RedisSerializer<Object> buildRedisSerializer() {
        ObjectMapper objectMapper = JsonUtils.createObjectMapper();
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    public StringRedisSerializer stringRedisSerializer() {
        return new StringRedisSerializer();
    }
}
