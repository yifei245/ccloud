package com.ccloud.framework.security.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PermissionCacheRefresherTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final SecurityFrameworkServiceImpl securityService = mock(SecurityFrameworkServiceImpl.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<SecurityFrameworkServiceImpl> provider = mock(ObjectProvider.class);
    private final PermissionCacheRefresher refresher = new PermissionCacheRefresher(redisTemplate, provider);

    @AfterEach
    void clearSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 无事务时立即发布() {
        refresher.publish(12L);
        verify(redisTemplate).convertAndSend(PermissionCacheRefresher.CHANNEL, "12");
        refresher.publish(null);
        verify(redisTemplate).convertAndSend(PermissionCacheRefresher.CHANNEL, "");
    }

    @Test
    void 事务中推迟到提交之后发布() {
        TransactionSynchronizationManager.initSynchronization();
        refresher.publish(7L);
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
        verify(redisTemplate).convertAndSend(PermissionCacheRefresher.CHANNEL, "7");
    }

    @Test
    void 收到消息后清理对应用户或全部缓存() {
        when(provider.getIfAvailable()).thenReturn(securityService);

        refresher.onMessage(message("12"), null);
        verify(securityService).invalidate(12L);

        refresher.onMessage(message(""), null);
        verify(securityService).invalidate(null);
    }

    @Test
    void 本节点没有权限表达式Bean时忽略消息() {
        when(provider.getIfAvailable()).thenReturn(null);
        refresher.onMessage(message("1"), null);
        verifyNoInteractions(securityService);
    }

    private static DefaultMessage message(String body) {
        return new DefaultMessage(PermissionCacheRefresher.CHANNEL.getBytes(StandardCharsets.UTF_8),
                body.getBytes(StandardCharsets.UTF_8));
    }
}
