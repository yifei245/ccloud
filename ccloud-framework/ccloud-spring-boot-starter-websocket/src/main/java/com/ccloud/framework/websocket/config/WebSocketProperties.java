package com.ccloud.framework.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 配置
 */
@ConfigurationProperties(prefix = "ccloud.websocket")
@Data
public class WebSocketProperties {

    private boolean enable = true;

    private String path = "/ws";

    /**
     * 发送器类型：local 仅推送本节点，redis 经广播覆盖全部节点。
     * 多实例部署必须用 redis，否则只有恰好持有该连接的那个节点能推送成功。
     */
    private String senderType = "redis";

    private String[] allowedOrigins = {"*"};
}
