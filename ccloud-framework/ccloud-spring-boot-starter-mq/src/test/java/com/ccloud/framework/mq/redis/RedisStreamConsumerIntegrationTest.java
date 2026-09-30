package com.ccloud.framework.mq.redis;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.mq.redis.core.AbstractRedisStreamMessageListener;
import com.ccloud.framework.mq.redis.core.RedisStreamMessageConsumer;
import com.ccloud.framework.mq.redis.core.RedisStreamMessageSender;
import com.ccloud.framework.mq.redis.core.RedisStreamPendingMessageResendJob;
import com.ccloud.framework.test.core.ut.LocalRedis;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Redis Stream 消费：至少一次投递。成功才 ACK；失败留在待处理列表，由重投任务捞回；
 * 超过重试上限或消息体无法解析时 ACK 丢弃，避免一条坏消息永远占着队列
 */
class RedisStreamConsumerIntegrationTest {

    private static final String GROUP = "test-app";

    private static StringRedisTemplate redisTemplate;

    private RedisStreamMessageConsumer consumer;

    @BeforeAll
    static void startRedis() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(LocalRedis.host(), LocalRedis.port());
        configuration.setPassword(LocalRedis.password());
        configuration.setDatabase(LocalRedis.database());
        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet();
        factory.start();
        redisTemplate = new StringRedisTemplate(factory);
    }

    @BeforeEach
    void flush() {
        redisTemplate.execute(connection -> {
            // 只清测试专用的库：本机 Redis 与其他项目共用，不能 FLUSHALL
            connection.serverCommands().flushDb();
            return null;
        }, true);
    }

    @AfterEach
    void stopConsumer() {
        if (consumer != null) {
            consumer.stop();
        }
    }

    @Test
    void 发送的消息应被监听器收到_处理成功后ACK() {
        RecordingListener listener = new RecordingListener("demo.ok", 0);
        start(listener);

        new RedisStreamMessageSender(redisTemplate).send("demo.ok", "m1", JsonUtils.toJsonString(new DemoMessage(1L, "hi")));

        await().atMost(Duration.ofSeconds(10)).until(() -> listener.received.size() == 1);
        assertThat(listener.received.get(0)).isEqualTo(new DemoMessage(1L, "hi"));
        assertThat(listener.messageIds).containsExactly("m1");
        await().atMost(Duration.ofSeconds(5)).until(() -> pendingCount("demo.ok") == 0);
    }

    @Test
    void 消费者启动前发送的消息也应被消费() {
        new RedisStreamMessageSender(redisTemplate).send("demo.early", "m1", JsonUtils.toJsonString(new DemoMessage(2L, "early")));
        RecordingListener listener = new RecordingListener("demo.early", 0);
        start(listener);

        await().atMost(Duration.ofSeconds(10)).until(() -> listener.received.size() == 1);
    }

    @Test
    void 处理失败时不ACK_重投任务捞回后再次投递成功() {
        RecordingListener listener = new RecordingListener("demo.retry", 1);
        start(listener);
        new RedisStreamMessageSender(redisTemplate).send("demo.retry", "m1", JsonUtils.toJsonString(new DemoMessage(3L, "retry")));

        await().atMost(Duration.ofSeconds(10)).until(() -> listener.attempts.get() == 1);
        await().atMost(Duration.ofSeconds(5)).until(() -> pendingCount("demo.retry") == 1);
        assertThat(listener.received).isEmpty();

        String result = new RedisStreamPendingMessageResendJob(redisTemplate, GROUP, List.of(listener), Duration.ZERO, 3)
                .execute();

        assertThat(result).contains("重投 1 条");
        await().atMost(Duration.ofSeconds(10)).until(() -> listener.received.size() == 1);
        assertThat(listener.messageIds).containsExactly("m1");
        await().atMost(Duration.ofSeconds(5)).until(() -> pendingCount("demo.retry") == 0);
    }

    @Test
    void 超过重试上限应ACK丢弃不再投递() {
        RecordingListener listener = new RecordingListener("demo.dead", Integer.MAX_VALUE);
        start(listener);
        new RedisStreamMessageSender(redisTemplate).send("demo.dead", "m1", JsonUtils.toJsonString(new DemoMessage(4L, "dead")));
        RedisStreamPendingMessageResendJob job =
                new RedisStreamPendingMessageResendJob(redisTemplate, GROUP, List.of(listener), Duration.ZERO, 2);

        for (int attempt = 1; attempt <= 3; attempt++) {
            int expected = attempt;
            await().atMost(Duration.ofSeconds(10)).until(() -> listener.attempts.get() == expected);
            await().atMost(Duration.ofSeconds(5)).until(() -> pendingCount("demo.dead") == 1);
            job.execute();
        }

        // 首次 + 重投 2 次后放弃
        assertThat(pendingCount("demo.dead")).isZero();
        assertThat(job.execute()).contains("重投 0 条");
        assertThat(listener.attempts.get()).isEqualTo(3);
    }

    @Test
    void 消息体无法解析时应ACK丢弃且不影响后续消息() {
        RecordingListener listener = new RecordingListener("demo.poison", 0);
        start(listener);
        RedisStreamMessageSender sender = new RedisStreamMessageSender(redisTemplate);
        sender.send("demo.poison", "bad", "{not json");
        sender.send("demo.poison", "good", JsonUtils.toJsonString(new DemoMessage(5L, "good")));

        await().atMost(Duration.ofSeconds(10)).until(() -> listener.received.size() == 1);
        assertThat(listener.messageIds).containsExactly("good");
        await().atMost(Duration.ofSeconds(5)).until(() -> pendingCount("demo.poison") == 0);
    }

    /**
     * 服务重启时消费组已存在。应用实际使用 Redisson 的连接工厂，它抛出的异常类型与 Lettuce 不同
     * （InvalidDataAccessApiUsageException 而非 RedisSystemException），只按 Lettuce 判断会让服务第二次启动失败
     */
    @Test
    void 使用Redisson连接时消费组已存在也应正常启动_服务重启场景() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + LocalRedis.host() + ":" + LocalRedis.port())
                .setPassword(LocalRedis.password()).setDatabase(LocalRedis.database());
        RedissonClient redisson = Redisson.create(config);
        try {
            StringRedisTemplate redissonTemplate = new StringRedisTemplate(new RedissonConnectionFactory(redisson));
            RecordingListener listener = new RecordingListener("demo.restart", 0);

            RedisStreamMessageConsumer first = new RedisStreamMessageConsumer(redissonTemplate, GROUP, List.of(listener));
            first.start();
            first.stop();
            RedisStreamMessageConsumer second = new RedisStreamMessageConsumer(redissonTemplate, GROUP, List.of(listener));
            second.start();
            try {
                new RedisStreamMessageSender(redissonTemplate).send("demo.restart", "m1",
                        JsonUtils.toJsonString(new DemoMessage(6L, "restart")));
                await().atMost(Duration.ofSeconds(10)).until(() -> listener.received.size() == 1);
            } finally {
                second.stop();
            }
        } finally {
            redisson.shutdown();
        }
    }

    private void start(AbstractRedisStreamMessageListener<?> listener) {
        consumer = new RedisStreamMessageConsumer(redisTemplate, GROUP, List.of(listener));
        consumer.start();
    }

    private static long pendingCount(String topic) {
        var summary = redisTemplate.opsForStream().pending(RedisStreamMessageSender.streamKey(topic), GROUP);
        return summary == null ? 0 : summary.getTotalPendingMessages();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemoMessage {
        private Long id;
        private String text;
    }

    /**
     * 前 failTimes 次处理抛异常，之后记录收到的消息
     */
    static class RecordingListener extends AbstractRedisStreamMessageListener<DemoMessage> {

        final List<DemoMessage> received = new CopyOnWriteArrayList<>();
        final List<String> messageIds = new CopyOnWriteArrayList<>();
        final AtomicInteger attempts = new AtomicInteger();
        private final int failTimes;

        RecordingListener(String topic, int failTimes) {
            super(topic, DemoMessage.class);
            this.failTimes = failTimes;
        }

        @Override
        public void onMessage(String messageId, DemoMessage message) {
            if (attempts.incrementAndGet() <= failTimes) {
                throw new IllegalStateException("模拟处理失败");
            }
            received.add(message);
            messageIds.add(messageId);
        }
    }
}
