package com.ccloud.module.open.service.app;

import com.ccloud.module.open.enums.app.OpenAppCacheConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 通知各网关实例清理应用快照缓存。处于事务中时推迟到提交之后：提交前广播，网关可能立即重新加载到旧数据并再次缓存。
 *
 * <p>发布失败（Redis 故障）只打警告、不向调用方抛出：此时库已提交，抛出会让接口返回失败（例如重置密钥后管理员看不到新密钥）；
 * 网关的快照缓存最多保留 TTL 后自行过期，以此兜底
 */
@Slf4j
@Component
public class OpenAppCacheInvalidator {

    private final StringRedisTemplate stringRedisTemplate;

    public OpenAppCacheInvalidator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void publish(String appKey) {
        send(appKey);
    }

    /**
     * 接口目录变化影响所有应用的快照
     */
    public void publishAll() {
        send("");
    }

    private void send(String message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcast(message);
                }
            });
            return;
        }
        broadcast(message);
    }

    private void broadcast(String message) {
        try {
            stringRedisTemplate.convertAndSend(OpenAppCacheConstants.INVALIDATE_CHANNEL, message);
        } catch (RuntimeException e) {
            log.warn("[open-app-cache] 发布缓存失效通知失败，网关缓存将在过期后刷新，appKey={}", message, e);
        }
    }
}
