package com.ccloud.framework.websocket.core.handler;

import com.ccloud.framework.websocket.core.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 连接生命周期处理：建立时登记会话，断开时移除。
 */
@Slf4j
@RequiredArgsConstructor
public class JsonWebSocketMessageHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionManager.addSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 必须移除：不移除会让会话表随着连接反复建立而无限增长
        sessionManager.removeSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 当前仅做服务端推送，客户端上行消息暂不处理
        log.debug("[handleTextMessage] 收到来自会话 {} 的消息", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[handleTransportError] 会话 {} 传输异常", session.getId(), exception);
        sessionManager.removeSession(session);
    }
}
