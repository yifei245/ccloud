package com.ccloud.framework.websocket.core;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.websocket.core.session.WebSocketSessionManagerImpl;
import com.ccloud.framework.websocket.core.util.WebSocketFrameworkUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketSessionManagerImplTest {

    private WebSocketSessionManagerImpl manager;

    @BeforeEach
    void setUp() {
        manager = new WebSocketSessionManagerImpl();
    }

    private WebSocketSession session(String id, Long userId, Integer userType) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        Map<String, Object> attributes = new HashMap<>();
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(userType);
        loginUser.setTenantId(1L);
        loginUser.setExpiresTime(LocalDateTime.now().plusHours(1));
        attributes.put(WebSocketFrameworkUtils.ATTRIBUTE_LOGIN_USER, loginUser);
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    @Test
    @DisplayName("添加会话后应能按用户查到")
    void addSession_shouldBeQueryableByUser() {
        manager.addSession(session("s1", 100L, UserTypeEnum.ADMIN.getValue()));

        assertThat(manager.getSessionList(UserTypeEnum.ADMIN.getValue(), 100L)).hasSize(1);
    }

    @Test
    @DisplayName("同一用户多端登录应保留多个会话，推送要送达每一端")
    void sameUserMultipleDevices_shouldKeepAllSessions() {
        manager.addSession(session("app", 100L, UserTypeEnum.MEMBER.getValue()));
        manager.addSession(session("web", 100L, UserTypeEnum.MEMBER.getValue()));

        assertThat(manager.getSessionList(UserTypeEnum.MEMBER.getValue(), 100L)).hasSize(2);
    }

    @Test
    @DisplayName("不同用户类型的同一编号应互不干扰——会员 100 与管理员 100 是两个人")
    void sameIdDifferentUserType_shouldNotCollide() {
        manager.addSession(session("m", 100L, UserTypeEnum.MEMBER.getValue()));
        manager.addSession(session("a", 100L, UserTypeEnum.ADMIN.getValue()));

        assertThat(manager.getSessionList(UserTypeEnum.MEMBER.getValue(), 100L)).hasSize(1);
        assertThat(manager.getSessionList(UserTypeEnum.ADMIN.getValue(), 100L)).hasSize(1);
    }

    @Test
    @DisplayName("移除会话后应查不到")
    void removeSession_shouldMakeItUnreachable() {
        WebSocketSession session = session("s1", 100L, UserTypeEnum.ADMIN.getValue());
        manager.addSession(session);
        manager.removeSession(session);

        assertThat(manager.getSessionList(UserTypeEnum.ADMIN.getValue(), 100L)).isEmpty();
    }

    @Test
    @DisplayName("移除一端不应影响该用户的其他端")
    void removeOneSession_shouldKeepOthers() {
        WebSocketSession app = session("app", 100L, UserTypeEnum.MEMBER.getValue());
        manager.addSession(app);
        manager.addSession(session("web", 100L, UserTypeEnum.MEMBER.getValue()));

        manager.removeSession(app);

        assertThat(manager.getSessionList(UserTypeEnum.MEMBER.getValue(), 100L))
                .hasSize(1)
                .allSatisfy(s -> assertThat(s.getId()).isEqualTo("web"));
    }

    @Test
    @DisplayName("按用户类型可取到该类型下的全部会话，用于全员广播")
    void getSessionListByUserType_shouldReturnAllOfThatType() {
        manager.addSession(session("a1", 100L, UserTypeEnum.ADMIN.getValue()));
        manager.addSession(session("a2", 200L, UserTypeEnum.ADMIN.getValue()));
        manager.addSession(session("m1", 300L, UserTypeEnum.MEMBER.getValue()));

        assertThat(manager.getSessionList(UserTypeEnum.ADMIN.getValue())).hasSize(2);
        assertThat(manager.getSessionList(UserTypeEnum.MEMBER.getValue())).hasSize(1);
    }

    @Test
    @DisplayName("查询不存在的用户应返回空集合而非 null")
    void unknownUser_shouldReturnEmptyCollection() {
        assertThat(manager.getSessionList(UserTypeEnum.ADMIN.getValue(), 999L)).isEmpty();
        assertThat(manager.getSessionList(UserTypeEnum.ADMIN.getValue())).isEmpty();
    }

    @Test
    @DisplayName("无登录身份的会话不应被纳入管理，避免向未认证连接推送数据")
    void sessionWithoutLoginUser_shouldBeIgnored() {
        WebSocketSession anonymous = mock(WebSocketSession.class);
        when(anonymous.getId()).thenReturn("anon");
        when(anonymous.getAttributes()).thenReturn(new HashMap<>());

        manager.addSession(anonymous);

        assertThat(manager.getSessionList(UserTypeEnum.ADMIN.getValue())).isEmpty();
    }
}
