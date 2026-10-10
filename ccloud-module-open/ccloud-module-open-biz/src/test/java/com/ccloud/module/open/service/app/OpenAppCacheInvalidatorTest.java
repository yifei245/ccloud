package com.ccloud.module.open.service.app;

import com.ccloud.module.open.enums.app.OpenAppCacheConstants;
import com.ccloud.module.open.test.BaseOpenDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@Import(OpenAppCacheInvalidator.class)
class OpenAppCacheInvalidatorTest extends BaseOpenDbAndRedisUnitTest {

    @Resource
    private OpenAppCacheInvalidator invalidator;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisConnectionFactory connectionFactory;
    @Resource
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 非事务中立即发布_事务中提交后才发布_回滚不发布() throws Exception {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        try {
            BlockingQueue<String> received = subscribe(container);
            invalidator.publish("k1");
            assertThat(received.poll(3, TimeUnit.SECONDS)).isEqualTo("k1");

            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(status -> {
                invalidator.publish("k2");
                assertThat(received.poll()).isNull();   // 提交前没有发布
            });
            assertThat(received.poll(3, TimeUnit.SECONDS)).isEqualTo("k2");

            tx.executeWithoutResult(status -> {
                invalidator.publishAll();
                status.setRollbackOnly();
            });
            assertThat(received.poll(1, TimeUnit.SECONDS)).isNull();

            invalidator.publishAll();
            assertThat(received.poll(3, TimeUnit.SECONDS)).isEmpty();
        } finally {
            container.stop();
            container.destroy();
        }
    }

    private BlockingQueue<String> subscribe(RedisMessageListenerContainer container) throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> queue.add(new String(message.getBody(), UTF_8)),
                new ChannelTopic(OpenAppCacheConstants.INVALIDATE_CHANNEL));
        container.afterPropertiesSet();
        container.start();
        Thread.sleep(300);   // 等订阅生效
        return queue;
    }
}
