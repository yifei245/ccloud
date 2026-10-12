package com.ccloud.module.open.service.app;

import com.ccloud.module.open.enums.app.OpenAppCacheConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 发布失败（Redis 故障）不能向调用方抛出：库已提交，接口却返回失败，管理员会看不到重置后的新密钥；
 * 网关缓存最多保留 TTL（60 秒）后自行过期
 */
class OpenAppCacheInvalidatorFailureTest {

    private final StringRedisTemplate template = mock(StringRedisTemplate.class);
    private final OpenAppCacheInvalidator invalidator = new OpenAppCacheInvalidator(template);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 非事务中发布失败不抛出() {
        failOnSend();

        assertThatCode(() -> invalidator.publish("k1")).doesNotThrowAnyException();
        assertThatCode(invalidator::publishAll).doesNotThrowAnyException();
        verify(template).convertAndSend(OpenAppCacheConstants.INVALIDATE_CHANNEL, "k1");
    }

    @Test
    void 事务提交后发布失败不抛出() {
        failOnSend();
        TransactionSynchronizationManager.initSynchronization();

        invalidator.publish("k2");
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

        assertThatCode(() -> synchronizations.forEach(TransactionSynchronization::afterCommit))
                .doesNotThrowAnyException();
        verify(template).convertAndSend(OpenAppCacheConstants.INVALIDATE_CHANNEL, "k2");
    }

    private void failOnSend() {
        doThrow(new RedisConnectionFailureException("redis down")).when(template).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
