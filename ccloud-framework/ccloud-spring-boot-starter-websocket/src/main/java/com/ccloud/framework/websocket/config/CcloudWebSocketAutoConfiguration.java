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
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

/**
 * WebSocket 自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(WebSocketProperties.class)
@ConditionalOnProperty(prefix = "ccloud.websocket", value = "enable", matchIfMissing = true)
@EnableWebSocket
public class CcloudWebSocketAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WebSocketSessionManager webSocketSessionManager() {
        return new WebSocketSessionManagerImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebSocketMessageSender webSocketMessageSender(WebSocketSessionManager sessionManager) {
        return new LocalWebSocketMessageSender(sessionManager);
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
