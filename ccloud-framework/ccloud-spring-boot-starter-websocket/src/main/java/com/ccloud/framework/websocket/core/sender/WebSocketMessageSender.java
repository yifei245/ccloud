package com.ccloud.framework.websocket.core.sender;

/**
 * WebSocket 消息推送
 */
public interface WebSocketMessageSender {

    /**
     * 推送给指定用户的全部在线端
     */
    void send(Integer userType, Long userId, String type, String content);

    /**
     * 推送给某类用户的全部在线连接
     */
    void send(Integer userType, String type, String content);

    /**
     * 推送给指定会话
     */
    void send(String sessionId, String type, String content);

    /**
     * 对象会被序列化为 JSON 后推送
     */
    void sendObject(Integer userType, Long userId, String type, Object content);
}
