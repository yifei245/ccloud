package com.ccloud.framework.websocket.core.sender;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.websocket.core.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Map;

/**
 * 只推送本节点会话的发送器。
 *
 * <p>单实例部署可用；多实例下会出现"消息发了但用户没收到"——
 * 用户的连接可能落在另一个节点上。多实例请用 {@code RedisWebSocketMessageSender}。
 */
@Slf4j
@RequiredArgsConstructor
public class LocalWebSocketMessageSender implements WebSocketMessageSender {

    private final WebSocketSessionManager sessionManager;

    @Override
    public void send(Integer userType, Long userId, String type, String content) {
        doSend(sessionManager.getSessionList(userType, userId), type, content);
    }

    @Override
    public void send(Integer userType, String type, String content) {
        doSend(sessionManager.getSessionList(userType), type, content);
    }

    @Override
    public void send(String sessionId, String type, String content) {
        WebSocketSession session = sessionManager.getSession(sessionId);
        if (session != null) {
            doSend(java.util.List.of(session), type, content);
        }
    }

    @Override
    public void sendObject(Integer userType, Long userId, String type, Object content) {
        send(userType, userId, type, JsonUtils.toJsonString(content));
    }

    private void doSend(Collection<WebSocketSession> sessions, String type, String content) {
        String payload = JsonUtils.toJsonString(Map.of("type", type, "content", content));
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                // WebSocketSession 不是线程安全的，并发发送会导致消息交错损坏
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (Exception e) {
                log.warn("[doSend] 向会话 {} 推送失败", session.getId(), e);
            }
        }
    }
}
