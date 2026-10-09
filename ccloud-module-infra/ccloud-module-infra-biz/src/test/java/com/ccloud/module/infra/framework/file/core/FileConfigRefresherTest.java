package com.ccloud.module.infra.framework.file.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FileConfigRefresherTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final FileClientFactory fileClientFactory = mock(FileClientFactory.class);
    private final FileConfigRefresher refresher = new FileConfigRefresher(redisTemplate, fileClientFactory);

    @AfterEach
    void clearSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 无事务时立即发布() {
        refresher.publish();
        verify(redisTemplate).convertAndSend(FileConfigRefresher.CHANNEL, "");
    }

    @Test
    void 事务中推迟到提交之后发布() {
        TransactionSynchronizationManager.initSynchronization();
        refresher.publish();
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
        verify(redisTemplate).convertAndSend(FileConfigRefresher.CHANNEL, "");
    }

    @Test
    void 事务回滚时不发布() {
        TransactionSynchronizationManager.initSynchronization();
        refresher.publish();
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void 收到消息后清空存储客户端缓存() {
        refresher.onMessage(new DefaultMessage(FileConfigRefresher.CHANNEL.getBytes(StandardCharsets.UTF_8),
                new byte[0]), null);
        verify(fileClientFactory).invalidateAll();
    }
}
