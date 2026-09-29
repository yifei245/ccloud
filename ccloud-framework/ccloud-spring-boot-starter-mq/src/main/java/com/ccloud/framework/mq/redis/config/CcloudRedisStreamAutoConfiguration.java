package com.ccloud.framework.mq.redis.config;

import com.ccloud.framework.mq.redis.core.AbstractRedisStreamMessageListener;
import com.ccloud.framework.mq.redis.core.RedisStreamMessageConsumer;
import com.ccloud.framework.mq.redis.core.RedisStreamPendingMessageResendJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

/**
 * Redis Stream 消费自动配置：存在监听器 Bean 时启动消费者，并提供待处理消息重投任务。
 * 重投任务需由业务服务注册为定时任务（XXL-Job）
 */
@AutoConfiguration(afterName = {"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "com.ccloud.framework.redis.config.CcloudRedisAutoConfiguration"})
@ConditionalOnBean({StringRedisTemplate.class, AbstractRedisStreamMessageListener.class})
public class CcloudRedisStreamAutoConfiguration {

    @Bean
    public RedisStreamMessageConsumer redisStreamMessageConsumer(
            StringRedisTemplate stringRedisTemplate, List<AbstractRedisStreamMessageListener<?>> listeners,
            @Value("${spring.application.name}") String applicationName) {
        return new RedisStreamMessageConsumer(stringRedisTemplate, applicationName, listeners);
    }

    @Bean
    public RedisStreamPendingMessageResendJob redisStreamPendingMessageResendJob(
            StringRedisTemplate stringRedisTemplate, List<AbstractRedisStreamMessageListener<?>> listeners,
            @Value("${spring.application.name}") String applicationName,
            @Value("${ccloud.mq.redis.pending-idle-timeout:5m}") Duration idleTimeout,
            @Value("${ccloud.mq.redis.max-retry:5}") int maxRetry) {
        return new RedisStreamPendingMessageResendJob(stringRedisTemplate, applicationName, listeners,
                idleTimeout, maxRetry);
    }
}
