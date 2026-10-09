package com.ccloud.framework.mq.redis;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.mq.outbox.core.DeadLetterDO;
import com.ccloud.framework.mq.outbox.core.DeadLetterMapper;
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
import org.mockito.ArgumentCaptor;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

        String result = new RedisStreamPendingMessageResendJob(redisTemplate, GROUP, List.of(listener), Duration.ZERO, 3, noDeadLetterMapper())
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
        String payload = JsonUtils.toJsonString(new DemoMessage(4L, "dead"));
        new RedisStreamMessageSender(redisTemplate).send("demo.dead", "m1", payload);
        DeadLetterMapper deadLetterMapper = mock(DeadLetterMapper.class);
        RedisStreamPendingMessageResendJob job = new RedisStreamPendingMessageResendJob(redisTemplate, GROUP,
                List.of(listener), Duration.ZERO, 2, deadLetterProvider(deadLetterMapper));

        for (int attempt = 1; attempt <= 3; attempt++) {
            int expected = attempt;
            await().atMost(Duration.ofSeconds(10)).until(() -> listener.attempts.get() == expected);
            await().atMost(Duration.ofSeconds(5)).until(() -> pendingCount("demo.dead") == 1);
            // 未达上限的重投不写死信
            if (attempt <= 2) {
                job.execute();
                verify(deadLetterMapper, never()).insert(any(DeadLetterDO.class));
            }
        }
        job.execute();

        // 首次 + 重投 2 次后放弃
        assertThat(pendingCount("demo.dead")).isZero();
        assertThat(job.execute()).contains("重投 0 条");
        assertThat(listener.attempts.get()).isEqualTo(3);

        ArgumentCaptor<DeadLetterDO> captor = ArgumentCaptor.forClass(DeadLetterDO.class);
        verify(deadLetterMapper).insert(captor.capture());
        DeadLetterDO deadLetter = captor.getValue();
        assertThat(deadLetter.getMessageId()).isEqualTo("m1");
        assertThat(deadLetter.getTopic()).isEqualTo("demo.dead");
        assertThat(deadLetter.getPayload()).isEqualTo(payload);
        assertThat(deadLetter.getTenantId()).isZero();
        assertThat(deadLetter.getRetryCount()).isEqualTo(2);
        assertThat(deadLetter.getErrorMsg()).isEqualTo("消费重试 2 次仍失败");
        assertThat(deadLetter.getHandled()).isFalse();
    }

    /**
     * 写死信失败（如库不可用、字段超长）时不能让整轮任务中断：待处理列表每轮都从头取，
     * 否则这条消息会永远排在队首，同一服务的其他消息与其他主题都得不到重投
     */
    @Test
    void 写死信失败时跳过该条且不ACK_后续消息与其他主题照常处理() {
        RecordingListener failing = new RecordingListener("demo.dl.fail", Integer.MAX_VALUE);
        RecordingListener other = new RecordingListener("demo.dl.other", Integer.MAX_VALUE);
        consumer = new RedisStreamMessageConsumer(redisTemplate, GROUP, List.of(failing, other));
        consumer.start();
        RedisStreamMessageSender sender = new RedisStreamMessageSender(redisTemplate);
        String payload = JsonUtils.toJsonString(new DemoMessage(7L, "dl"));
        sender.send("demo.dl.fail", "bad", payload);
        sender.send("demo.dl.fail", "good", payload);
        sender.send("demo.dl.other", "other", payload);
        await().atMost(Duration.ofSeconds(10)).until(() -> failing.attempts.get() == 2 && other.attempts.get() == 1);
        await().atMost(Duration.ofSeconds(5)).until(() -> pendingCount("demo.dl.fail") == 2 && pendingCount("demo.dl.other") == 1);

        DeadLetterMapper deadLetterMapper = mock(DeadLetterMapper.class);
        doThrow(new IllegalStateException("写库失败")).when(deadLetterMapper)
                .insert(argThat((DeadLetterDO deadLetter) -> "bad".equals(deadLetter.getMessageId())));
        RedisStreamPendingMessageResendJob job = new RedisStreamPendingMessageResendJob(redisTemplate, GROUP,
                List.of(failing, other), Duration.ZERO, 0, deadLetterProvider(deadLetterMapper));

        String result = job.execute();

        assertThat(result).contains("丢弃 2 条").contains("失败 1 条");
        ArgumentCaptor<DeadLetterDO> captor = ArgumentCaptor.forClass(DeadLetterDO.class);
        verify(deadLetterMapper, times(3)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(DeadLetterDO::getMessageId).containsExactly("bad", "good", "other");
        // 失败的那条留在待处理列表，下一轮再试
        assertThat(pendingCount("demo.dl.fail")).isEqualTo(1);
        assertThat(pendingCount("demo.dl.other")).isZero();
        assertThat(job.execute()).contains("失败 1 条");
    }

    @Test
    void 超长字段写死信前按列宽截断() {
        RecordingListener listener = new RecordingListener("demo.dl.long", Integer.MAX_VALUE);
        start(listener);
        String messageId = "m".repeat(100);
        // 消息体须能解析，否则消费端直接 ACK 丢弃，进不了重投
        new RedisStreamMessageSender(redisTemplate).send("demo.dl.long", messageId,
                JsonUtils.toJsonString(new DemoMessage(8L, "x".repeat(70_000))));
        await().atMost(Duration.ofSeconds(10)).until(() -> listener.attempts.get() == 1);
        await().atMost(Duration.ofSeconds(5)).until(() -> pendingCount("demo.dl.long") == 1);
        DeadLetterMapper deadLetterMapper = mock(DeadLetterMapper.class);

        new RedisStreamPendingMessageResendJob(redisTemplate, GROUP, List.of(listener), Duration.ZERO, 0,
                deadLetterProvider(deadLetterMapper)).execute();

        ArgumentCaptor<DeadLetterDO> captor = ArgumentCaptor.forClass(DeadLetterDO.class);
        verify(deadLetterMapper).insert(captor.capture());
        assertThat(captor.getValue().getMessageId()).hasSize(DeadLetterDO.MESSAGE_ID_MAX_LENGTH);
        assertThat(captor.getValue().getPayload()).hasSize(DeadLetterDO.PAYLOAD_MAX_BYTES);
        assertThat(pendingCount("demo.dl.long")).isZero();
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

    private static ObjectProvider<DeadLetterMapper> noDeadLetterMapper() {
        return new StaticListableBeanFactory().getBeanProvider(DeadLetterMapper.class);
    }

    private static ObjectProvider<DeadLetterMapper> deadLetterProvider(DeadLetterMapper mapper) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("deadLetterMapper", mapper);
        return beanFactory.getBeanProvider(DeadLetterMapper.class);
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
