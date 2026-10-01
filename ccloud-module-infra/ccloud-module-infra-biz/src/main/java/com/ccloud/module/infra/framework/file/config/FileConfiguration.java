package com.ccloud.module.infra.framework.file.config;

import com.ccloud.module.infra.framework.file.core.FileConfigRefresher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration(proxyBeanMethods = false)
public class FileConfiguration {

    @Bean
    public RedisMessageListenerContainer fileConfigRefreshListenerContainer(RedisConnectionFactory connectionFactory,
                                                                             FileConfigRefresher refresher) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(refresher, new ChannelTopic(FileConfigRefresher.CHANNEL));
        return container;
    }
}
