package com.ccloud.framework.websocket.config;

import com.ccloud.framework.websocket.core.handler.JsonWebSocketMessageHandler;
import com.ccloud.framework.websocket.core.security.LoginUserHandshakeInterceptor;
import com.ccloud.framework.websocket.core.sender.LocalWebSocketMessageSender;
import com.ccloud.framework.websocket.core.sender.WebSocketMessageSender;
import com.ccloud.framework.websocket.core.session.WebSocketSessionManager;
import com.ccloud.framework.websocket.core.session.WebSocketSessionManagerImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import com.ccloud.framework.websocket.core.sender.RedisWebSocketMessageSender;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

/**
 * WebSocket 自动配置
 */
@AutoConfiguration(afterName = {"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "com.ccloud.framework.redis.config.CcloudRedisAutoConfiguration"})
@EnableConfigurationProperties(WebSocketProperties.class)
@ConditionalOnProperty(prefix = "ccloud.websocket", value = "enable", matchIfMissing = true)
@EnableWebSocket
public class CcloudWebSocketAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WebSocketSessionManager webSocketSessionManager() {
        return new WebSocketSessionManagerImpl();
    }

    /**
     * 仅推送本节点连接，只适合单实例部署
     */
    @Bean
    @ConditionalOnMissingBean(WebSocketMessageSender.class)
    @ConditionalOnProperty(prefix = "ccloud.websocket", value = "sender-type", havingValue = "local")
    public WebSocketMessageSender localWebSocketMessageSender(WebSocketSessionManager sessionManager) {
        return new LocalWebSocketMessageSender(sessionManager);
    }

    /**
     * 默认：经 Redis 广播到全部节点。缺少 Redis 时启动失败，而不是悄悄退化成只推本节点
     */
    @Bean
    @ConditionalOnMissingBean(WebSocketMessageSender.class)
    @ConditionalOnProperty(prefix = "ccloud.websocket", value = "sender-type", havingValue = "redis", matchIfMissing = true)
    public RedisWebSocketMessageSender redisWebSocketMessageSender(WebSocketSessionManager sessionManager,
                                                                   StringRedisTemplate stringRedisTemplate) {
        return new RedisWebSocketMessageSender(sessionManager, stringRedisTemplate);
    }

    @Bean
    @ConditionalOnBean(RedisWebSocketMessageSender.class)
    public RedisMessageListenerContainer webSocketRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory, RedisWebSocketMessageSender sender) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        sender.subscribe(container);
        return container;
    }

    @Bean
    public JsonWebSocketMessageHandler jsonWebSocketMessageHandler(WebSocketSessionManager sessionManager) {
        return new JsonWebSocketMessageHandler(sessionManager);
    }

    @Bean
    public LoginUserHandshakeInterceptor loginUserHandshakeInterceptor() {
        return new LoginUserHandshakeInterceptor();
    }

    /**
     * 端点注册。用独立的 configurer Bean 而非让本配置类实现 {@code WebSocketConfigurer}：
     * 后者的回调方法里拿不到注入的依赖，只能靠字段注入，可读性与可测性都更差。
     */
    @Bean
    public WebSocketConfigurer ccloudWebSocketConfigurer(WebSocketProperties properties,
                                                         JsonWebSocketMessageHandler handler,
                                                         LoginUserHandshakeInterceptor interceptor) {
        return registry -> registry.addHandler(handler, properties.getPath())
                .addInterceptors(interceptor)
                .setAllowedOriginPatterns(properties.getAllowedOrigins());
    }
}
