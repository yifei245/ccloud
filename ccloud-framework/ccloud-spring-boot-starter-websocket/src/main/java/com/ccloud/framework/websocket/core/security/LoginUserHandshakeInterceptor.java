package com.ccloud.framework.websocket.core.security;

import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.websocket.core.util.WebSocketFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 握手阶段的身份校验。
 *
 * <p>WebSocket 连接建立后就长期存在，身份必须在握手时确定——
 * 连上之后再校验等于给了一个无需认证的长连接通道。
 *
 * <p>身份来自 HTTP 握手请求，由网关下发的内部头经 {@code LoginUserFilter} 解析，
 * 因此这里直接读 SecurityContext 即可。
 */
@Slf4j
public class LoginUserHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }
        LoginUser loginUser = com.ccloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            log.info("[beforeHandshake] 未认证的连接请求被拒绝");
            return false;
        }
        // 把身份存进会话属性：SecurityContext 是线程级的，连接建立后就取不到了
        WebSocketFrameworkUtils.setLoginUser(loginUser, attributes);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
