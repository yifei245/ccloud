package com.ccloud.module.open.service.app;

import com.ccloud.module.open.enums.app.OpenAppCacheConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 通知各网关实例清理应用快照缓存。处于事务中时推迟到提交之后：提交前广播，网关可能立即重新加载到旧数据并再次缓存
 */
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
                    stringRedisTemplate.convertAndSend(OpenAppCacheConstants.INVALIDATE_CHANNEL, message);
                }
            });
            return;
        }
        stringRedisTemplate.convertAndSend(OpenAppCacheConstants.INVALIDATE_CHANNEL, message);
    }
}
