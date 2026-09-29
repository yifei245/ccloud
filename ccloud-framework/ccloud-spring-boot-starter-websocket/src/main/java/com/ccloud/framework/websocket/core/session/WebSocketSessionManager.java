package com.ccloud.framework.websocket.core.session;

import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;

/**
 * WebSocket 会话管理
 */
public interface WebSocketSessionManager {

    void addSession(WebSocketSession session);

    void removeSession(WebSocketSession session);

    WebSocketSession getSession(String id);

    /**
     * @return 指定用户的全部会话。同一用户可能在多端同时在线，推送要送达每一端
     */
    Collection<WebSocketSession> getSessionList(Integer userType, Long userId);

    /**
     * @return 指定用户类型的全部会话，用于全员广播
     */
    Collection<WebSocketSession> getSessionList(Integer userType);
}
