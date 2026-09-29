package com.ccloud.framework.i18n.config;

import com.ccloud.framework.i18n.core.DatabaseMessageSource;
import com.ccloud.framework.i18n.core.I18nMessageFrameworkService;
import com.ccloud.framework.i18n.core.I18nMessageRefreshScheduler;
import com.ccloud.framework.i18n.core.I18nMessageRefresher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;

/**
 * 数据库文案的加载与刷新。需要服务提供 {@link I18nMessageFrameworkService}：
 * system 服务直接查库，其他服务经 I18nMessageApi 向 system 获取；未提供时只使用 properties 基线
 */
@AutoConfiguration(after = CcloudI18nAutoConfiguration.class,
        afterName = {"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "com.ccloud.framework.redis.config.CcloudRedisAutoConfiguration"})
@ConditionalOnProperty(prefix = "ccloud.i18n", value = "enable", matchIfMissing = true)
@ConditionalOnBean({DatabaseMessageSource.class, I18nMessageFrameworkService.class, StringRedisTemplate.class})
public class CcloudI18nRefreshAutoConfiguration {

    @Bean
    public I18nMessageRefresher i18nMessageRefresher(DatabaseMessageSource messageSource,
                                                     I18nMessageFrameworkService messageService,
                                                     I18nProperties properties,
                                                     StringRedisTemplate stringRedisTemplate) {
        return new I18nMessageRefresher(messageSource, messageService, properties.getSupportedLocales(),
                stringRedisTemplate);
    }

    @Bean
    public RedisMessageListenerContainer i18nRedisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                          I18nMessageRefresher refresher) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(refresher, new ChannelTopic(I18nMessageRefresher.CHANNEL));
        return container;
    }

    @Bean
    public I18nMessageRefreshScheduler i18nMessageRefreshScheduler(
            I18nMessageRefresher refresher, @Value("${ccloud.i18n.refresh-interval:5m}") Duration interval) {
        return new I18nMessageRefreshScheduler(refresher, interval);
    }
}
