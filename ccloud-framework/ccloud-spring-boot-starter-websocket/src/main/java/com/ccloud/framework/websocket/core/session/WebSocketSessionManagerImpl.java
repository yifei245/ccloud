package com.ccloud.framework.websocket.core.session;

import com.ccloud.framework.websocket.core.util.WebSocketFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于内存的会话管理。
 *
 * <p>只管理<b>本节点</b>的连接。跨节点推送由 {@code RedisWebSocketMessageSender}
 * 广播后，各节点各自推送本地持有的会话。
 */
@Slf4j
public class WebSocketSessionManagerImpl implements WebSocketSessionManager {

    /**
     * sessionId -> 会话
     */
    private final Map<String, WebSocketSession> idSessions = new ConcurrentHashMap<>();

    /**
     * userType -> userId -> 会话列表。
     *
     * <p>必须按 userType 再分一层：会员 100 与管理员 100 是两个不同的人，
     * 只按 userId 索引会把消息推给错误的对象。
     */
    private final Map<Integer, Map<Long, List<WebSocketSession>>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void addSession(WebSocketSession session) {
        Long userId = WebSocketFrameworkUtils.getLoginUserId(session);
        Integer userType = WebSocketFrameworkUtils.getLoginUserType(session);
        // 未认证的连接不纳入管理，避免向匿名连接推送业务数据
        if (userId == null || userType == null) {
            log.warn("[addSession] 会话 {} 没有登录身份，已忽略", session.getId());
            return;
        }
        idSessions.put(session.getId(), session);
        userSessions.computeIfAbsent(userType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>())
                .add(session);
    }

    @Override
    public void removeSession(WebSocketSession session) {
        idSessions.remove(session.getId());
        Long userId = WebSocketFrameworkUtils.getLoginUserId(session);
        Integer userType = WebSocketFrameworkUtils.getLoginUserType(session);
        if (userId == null || userType == null) {
            return;
        }
        Map<Long, List<WebSocketSession>> byUser = userSessions.get(userType);
        if (byUser == null) {
            return;
        }
        List<WebSocketSession> sessions = byUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.removeIf(s -> s.getId().equals(session.getId()));
        // 用户已无在线会话时清掉条目，避免长期运行后 Map 里堆积大量空列表
        if (sessions.isEmpty()) {
            byUser.remove(userId);
        }
    }

    @Override
    public WebSocketSession getSession(String id) {
        return idSessions.get(id);
    }

    @Override
    public Collection<WebSocketSession> getSessionList(Integer userType, Long userId) {
        Map<Long, List<WebSocketSession>> byUser = userSessions.get(userType);
        if (byUser == null) {
            return List.of();
        }
        List<WebSocketSession> sessions = byUser.get(userId);
        return sessions == null ? List.of() : List.copyOf(sessions);
    }

    @Override
    public Collection<WebSocketSession> getSessionList(Integer userType) {
        Map<Long, List<WebSocketSession>> byUser = userSessions.get(userType);
        if (byUser == null) {
            return List.of();
        }
        return byUser.values().stream().flatMap(List::stream).toList();
    }
}
