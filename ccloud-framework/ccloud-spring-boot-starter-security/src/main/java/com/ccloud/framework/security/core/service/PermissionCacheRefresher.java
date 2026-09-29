package com.ccloud.framework.security.core.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;

/**
 * 权限缓存的跨节点刷新。
 *
 * <p>每个服务节点在 {@link SecurityFrameworkServiceImpl} 里有一份本地权限缓存。
 * 角色、菜单、授权关系变更后由 system 服务发布消息，所有节点（包括发布者自己）收到后清理本地缓存。
 * 消息体为用户编号，为空表示清理全部用户。
 */
@Slf4j
public class PermissionCacheRefresher implements MessageListener {

    public static final String CHANNEL = "ccloud:permission:refresh";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectProvider<SecurityFrameworkServiceImpl> securityFrameworkServiceProvider;

    public PermissionCacheRefresher(StringRedisTemplate stringRedisTemplate,
                                    ObjectProvider<SecurityFrameworkServiceImpl> securityFrameworkServiceProvider) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.securityFrameworkServiceProvider = securityFrameworkServiceProvider;
    }

    /**
     * 发布刷新消息。处于事务中时推迟到提交之后：提交前广播，其他节点可能重新加载到旧数据并再次缓存
     *
     * @param userId 为空表示全部用户
     */
    public void publish(Long userId) {
        String body = userId != null ? userId.toString() : "";
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stringRedisTemplate.convertAndSend(CHANNEL, body);
                }
            });
            return;
        }
        stringRedisTemplate.convertAndSend(CHANNEL, body);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        SecurityFrameworkServiceImpl service = securityFrameworkServiceProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        // 发布端的 StringRedisSerializer 不加引号；兼容 JSON 序列化器发出的带引号字符串
        body = StrUtil.strip(body.trim(), "\"");
        Long userId = StrUtil.isBlank(body) ? null : Long.valueOf(body);
        service.invalidate(userId);
        log.debug("[onMessage][清理权限缓存 userId={}]", userId);
    }
}
