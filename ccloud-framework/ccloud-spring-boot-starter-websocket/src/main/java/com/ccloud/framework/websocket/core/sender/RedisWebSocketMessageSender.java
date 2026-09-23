package com.ccloud.framework.websocket.core.sender;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.websocket.core.session.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

/**
 * 多节点推送：发送方把消息发布到 Redis 频道，每个节点收到后只推送给本节点持有的连接。
 *
 * <p>用 Pub/Sub 而非 Stream：推送是"尽力而为"，用户不在线时消息本就无处投递，
 * 离线期间的内容由业务侧（如站内信表）持久化，重新上线后自行拉取。
 */
@Slf4j
public class RedisWebSocketMessageSender implements WebSocketMessageSender, MessageListener {

    static final String CHANNEL = "ccloud:websocket:message";

    private final LocalWebSocketMessageSender localSender;
    private final StringRedisTemplate redisTemplate;

    public RedisWebSocketMessageSender(WebSocketSessionManager sessionManager, StringRedisTemplate redisTemplate) {
        this.localSender = new LocalWebSocketMessageSender(sessionManager);
        this.redisTemplate = redisTemplate;
    }

    /**
     * 订阅广播频道，由自动配置在启动时调用
     */
    public void subscribe(RedisMessageListenerContainer container) {
        container.addMessageListener(this, new ChannelTopic(CHANNEL));
    }

    @Override
    public void send(Integer userType, Long userId, String type, String content) {
        publish(new BroadcastMessage(null, userType, userId, type, content));
    }

    @Override
    public void send(Integer userType, String type, String content) {
        publish(new BroadcastMessage(null, userType, null, type, content));
    }

    @Override
    public void send(String sessionId, String type, String content) {
        publish(new BroadcastMessage(sessionId, null, null, type, content));
    }

    @Override
    public void sendObject(Integer userType, Long userId, String type, Object content) {
        send(userType, userId, type, JsonUtils.toJsonString(content));
    }

    private void publish(BroadcastMessage message) {
        redisTemplate.convertAndSend(CHANNEL, JsonUtils.toJsonString(message));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        BroadcastMessage broadcast;
        try {
            broadcast = JsonUtils.parseObject(new String(message.getBody(), StandardCharsets.UTF_8), BroadcastMessage.class);
        } catch (RuntimeException e) {
            log.warn("[onMessage] 无法解析的广播消息，忽略", e);
            return;
        }
        if (broadcast.sessionId() != null) {
            localSender.send(broadcast.sessionId(), broadcast.type(), broadcast.content());
        } else if (broadcast.userId() != null) {
            localSender.send(broadcast.userType(), broadcast.userId(), broadcast.type(), broadcast.content());
        } else {
            localSender.send(broadcast.userType(), broadcast.type(), broadcast.content());
        }
    }

    /**
     * 广播消息：sessionId、userId 决定推送范围，三者都为空时按 userType 推送
     */
    record BroadcastMessage(String sessionId, Integer userType, Long userId, String type, String content) {
    }
}
