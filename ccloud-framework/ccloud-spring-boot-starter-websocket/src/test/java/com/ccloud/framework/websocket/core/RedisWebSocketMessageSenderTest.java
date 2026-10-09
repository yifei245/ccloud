package com.ccloud.framework.websocket.core;

import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.test.core.ut.LocalRedis;
import com.ccloud.framework.websocket.core.sender.RedisWebSocketMessageSender;
import com.ccloud.framework.websocket.core.session.WebSocketSessionManagerImpl;
import com.ccloud.framework.websocket.core.util.WebSocketFrameworkUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.mockito.ArgumentMatcher;
import com.ccloud.framework.common.util.json.JsonUtils;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * 多节点推送：模拟两个节点共用一个 Redis，连接只在节点 B 上，消息从节点 A 发出
 */
class RedisWebSocketMessageSenderTest {

    private static LettuceConnectionFactory connectionFactory;

    private Node nodeA;
    private Node nodeB;

    @BeforeAll
    static void startRedis() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(LocalRedis.host(), LocalRedis.port());
        configuration.setPassword(LocalRedis.password());
        configuration.setDatabase(LocalRedis.database());
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
    }

    @BeforeEach
    void startNodes() {
        nodeA = new Node();
        nodeB = new Node();
    }

    @AfterEach
    void stopNodes() {
        nodeA.stop();
        nodeB.stop();
    }

    @Test
    void 按用户推送_连接在其他节点上也能收到() throws Exception {
        WebSocketSession session = session("s1", 7L, 1);
        nodeB.sessionManager.addSession(session);

        nodeA.sender.send(1, 7L, "notify-message", "{\"id\":1}");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                verify(session).sendMessage(argThat(payload("notify-message", "{\"id\":1}"))));
    }

    @Test
    void 按用户推送_不投递给其他用户() throws Exception {
        WebSocketSession target = session("s1", 7L, 1);
        WebSocketSession other = session("s2", 8L, 1);
        nodeB.sessionManager.addSession(target);
        nodeB.sessionManager.addSession(other);

        nodeA.sender.send(1, 7L, "t", "c");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(target).sendMessage(any()));
        verify(other, never()).sendMessage(any());
    }

    @Test
    void 按会话编号与按用户类型推送_跨节点生效() throws Exception {
        WebSocketSession admin = session("s1", 7L, 1);
        WebSocketSession member = session("s2", 9L, 2);
        nodeB.sessionManager.addSession(admin);
        nodeB.sessionManager.addSession(member);

        nodeA.sender.send("s2", "t", "by-session");
        nodeA.sender.send(1, "t", "by-type");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(member).sendMessage(argThat(payload("t", "by-session")));
            verify(admin).sendMessage(argThat(payload("t", "by-type")));
        });
        verify(member, never()).sendMessage(argThat(payload("t", "by-type")));
    }

    @Test
    void 各节点都没有对应连接时不报错() {
        nodeA.sender.send(1, 404L, "t", "c");
        nodeA.sender.send("missing", "t", "c");
    }

    /**
     * 推送体为 {"type": ..., "content": ...}，按内容比对，不依赖字段顺序
     */
    private static ArgumentMatcher<WebSocketMessage<?>> payload(String type, String content) {
        return message -> message instanceof TextMessage text
                && JsonUtils.parseObject(text.getPayload(), Map.class).equals(Map.of("type", type, "content", content));
    }

    private static WebSocketSession session(String id, Long userId, Integer userType) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(userType);
        loginUser.setTenantId(1L);
        loginUser.setExpiresTime(LocalDateTime.now().plusHours(1));
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebSocketFrameworkUtils.ATTRIBUTE_LOGIN_USER, loginUser);
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    /**
     * 一个服务节点：独立的会话管理器与订阅，共用 Redis
     */
    private static class Node {

        final WebSocketSessionManagerImpl sessionManager = new WebSocketSessionManagerImpl();
        final RedisWebSocketMessageSender sender;
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        Node() {
            StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
            sender = new RedisWebSocketMessageSender(sessionManager, redisTemplate);
            container.setConnectionFactory(connectionFactory);
            sender.subscribe(container);
            container.afterPropertiesSet();
            container.start();
            await().atMost(Duration.ofSeconds(5)).until(container::isListening);
        }

        void stop() {
            container.stop();
        }
    }
}
