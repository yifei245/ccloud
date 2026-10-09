package com.ccloud.framework.mq.outbox;

import com.ccloud.framework.mq.outbox.core.*;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 本地消息表的端到端验证。
 *
 * <p>最终一致性方案的正确性全部押在两条不变量上，本测试直接验证它们：
 * <ol>
 *   <li><b>业务与消息同生共死</b>——业务事务回滚时消息必须一并消失，
 *       否则会发出一条描述"从未发生过的事"的消息；</li>
 *   <li><b>消费幂等</b>——消息必然会重复投递，重复消费不能产生第二次副作用。</li>
 * </ol>
 */
@SpringBootTest(classes = OutboxIntegrationTest.TestApp.class)
@ActiveProfiles("unit-test")
class OutboxIntegrationTest {

    @Configuration
    @EnableAutoConfiguration
    // 不手动 @MapperScan：框架 Mapper 必须由 CcloudOutboxAutoConfiguration 自行注册，业务服务里没人会替它扫描
    static class TestApp {

        /**
         * 可控的投递器：能模拟投递成功与失败，无需真实 Redis
         */
        @Bean
        StubMessageSender stubMessageSender() {
            return new StubMessageSender();
        }

        @Bean
        DemoBizService demoBizService() {
            return new DemoBizService();
        }
    }

    /**
     * 模拟一段"写业务数据 + 发消息"的业务逻辑
     */
    public static class DemoBizService {

        @Autowired
        private OutboxMessageService outboxMessageService;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Transactional
        public void placeOrder(String orderNo, boolean failAfterSend) {
            jdbcTemplate.update("INSERT INTO demo_order (order_no) VALUES (?)", orderNo);
            outboxMessageService.saveAndPublish("order.created", java.util.Map.of("orderNo", orderNo));
            if (failAfterSend) {
                throw new IllegalStateException("业务在发消息之后失败");
            }
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DemoBizService bizService;
    @Autowired
    private OutboxMessageMapper outboxMapper;
    @Autowired
    private IdempotentConsumer idempotentConsumer;
    @Autowired
    private StubMessageSender sender;
    @Autowired
    private OutboxPublisher outboxPublisher;
    @Autowired
    private DeadLetterMapper deadLetterMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS demo_order");
        jdbcTemplate.execute("CREATE TABLE demo_order (order_no VARCHAR(64) PRIMARY KEY)");
        jdbcTemplate.execute("DELETE FROM infra_message_outbox");
        jdbcTemplate.execute("DELETE FROM infra_message_consume_log");
        jdbcTemplate.execute("DELETE FROM infra_message_dead_letter");
        sender.reset();
    }

    private int outboxCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infra_message_outbox", Integer.class);
    }

    // ========== 不变量一：业务与消息同生共死 ==========

    @Test
    @DisplayName("业务事务提交后，消息应已落库并投递成功")
    void commit_shouldPersistAndPublishMessage() {
        bizService.placeOrder("ORD-1", false);

        assertThat(outboxCount()).isEqualTo(1);
        List<OutboxMessageDO> messages = outboxMapper.selectList();
        assertThat(messages.get(0).getTopic()).isEqualTo("order.created");
        assertThat(messages.get(0).getStatus()).isEqualTo(OutboxStatusEnum.SUCCESS.getStatus());
        assertThat(sender.sentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("业务事务回滚时消息必须一并消失，绝不能发出描述未发生之事的消息")
    void rollback_shouldDiscardMessageEntirely() {
        assertThatThrownBy(() -> bizService.placeOrder("ORD-2", true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM demo_order", Integer.class)).isZero();
        assertThat(outboxCount()).isZero();
        assertThat(sender.sentCount()).isZero();
    }

    @Test
    @DisplayName("消息必须在事务提交之后才投递，提交前投递会让下游读到尚未可见的数据")
    void shouldPublishOnlyAfterCommit() {
        sender.setOnSend(() -> {
            Integer visible = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM demo_order WHERE order_no = 'ORD-3'", Integer.class);
            assertThat(visible).as("投递时业务数据必须已可见").isEqualTo(1);
        });

        bizService.placeOrder("ORD-3", false);

        assertThat(sender.sentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("投递失败时业务不受影响，消息标记为待重试并累加重试次数")
    void publishFailure_shouldNotAffectBusinessAndMarkForRetry() {
        sender.setFailing(true);

        bizService.placeOrder("ORD-4", false);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM demo_order", Integer.class)).isEqualTo(1);
        OutboxMessageDO message = outboxMapper.selectList().get(0);
        assertThat(message.getStatus()).isEqualTo(OutboxStatusEnum.FAILURE.getStatus());
        assertThat(message.getRetryCount()).isEqualTo(1);
        assertThat(message.getNextRetryTime()).isNotNull();
        assertThat(message.getErrorMsg()).isNotBlank();
    }

    // ========== 死信 ==========

    private OutboxMessageDO insertMessage(String messageId, Long tenantId, int retryCount, int maxRetry) {
        OutboxMessageDO message = new OutboxMessageDO();
        message.setMessageId(messageId);
        message.setTopic("order.created");
        message.setPayload("{\"orderNo\":\"" + messageId + "\"}");
        message.setStatus(OutboxStatusEnum.FAILURE.getStatus());
        message.setRetryCount(retryCount);
        message.setMaxRetry(maxRetry);
        message.setNextRetryTime(java.time.LocalDateTime.now());
        message.setTenantId(tenantId);
        outboxMapper.insert(message);
        return message;
    }

    @Test
    @DisplayName("重试达到上限时状态转为死信，并写入一条与原消息一致的死信记录")
    void exhaustedRetry_shouldWriteDeadLetter() {
        OutboxMessageDO message = insertMessage("DL-1", 7L, 4, 5);
        sender.setFailing(true);

        outboxPublisher.publish(message);

        assertThat(outboxMapper.selectById(message.getId()).getStatus()).isEqualTo(OutboxStatusEnum.DEAD.getStatus());
        List<DeadLetterDO> deadLetters = deadLetterMapper.selectList();
        assertThat(deadLetters).hasSize(1);
        DeadLetterDO deadLetter = deadLetters.get(0);
        assertThat(deadLetter.getMessageId()).isEqualTo("DL-1");
        assertThat(deadLetter.getTopic()).isEqualTo("order.created");
        assertThat(deadLetter.getPayload()).isEqualTo(message.getPayload());
        assertThat(deadLetter.getTenantId()).isEqualTo(7L);
        assertThat(deadLetter.getRetryCount()).isEqualTo(5);
        assertThat(deadLetter.getErrorMsg()).isEqualTo("模拟投递失败");
        assertThat(deadLetter.getHandled()).isFalse();
        assertThat(deadLetter.getHandledTime()).isNull();
    }

    @Test
    @DisplayName("重试未达上限时不写死信")
    void belowMaxRetry_shouldNotWriteDeadLetter() {
        OutboxMessageDO message = insertMessage("DL-2", 7L, 3, 5);
        sender.setFailing(true);

        outboxPublisher.publish(message);

        assertThat(outboxMapper.selectById(message.getId()).getStatus()).isEqualTo(OutboxStatusEnum.FAILURE.getStatus());
        assertThat(deadLetterMapper.selectCount()).isZero();
    }

    @Test
    @DisplayName("内存中的消息没有租户（刚插入未回读）时，死信的租户取自库中原消息")
    void exhaustedRetry_shouldTakeTenantFromStoredMessageWhenMissingInMemory() {
        OutboxMessageDO stored = insertMessage("DL-3", 9L, 0, 1);
        OutboxMessageDO inMemory = new OutboxMessageDO();
        inMemory.setId(stored.getId());
        inMemory.setMessageId("DL-3");
        inMemory.setTopic("order.created");
        inMemory.setPayload("{}");
        inMemory.setRetryCount(0);
        inMemory.setMaxRetry(1);
        sender.setFailing(true);

        outboxPublisher.publish(inMemory);

        assertThat(deadLetterMapper.selectList()).extracting(DeadLetterDO::getTenantId).containsExactly(9L);
    }

    @Test
    @DisplayName("消息编号、主题超出死信表列宽时截断后写入，不因写库失败卡在待重试")
    void exhaustedRetry_shouldTruncateOverlongFields() {
        OutboxMessageDO stored = insertMessage("DL-4", 9L, 0, 1);
        OutboxMessageDO inMemory = new OutboxMessageDO();
        inMemory.setId(stored.getId());
        inMemory.setMessageId("DL-4-" + "m".repeat(100));
        inMemory.setTopic("t".repeat(200));
        inMemory.setPayload("{}");
        inMemory.setTenantId(9L);
        inMemory.setRetryCount(0);
        inMemory.setMaxRetry(1);
        sender.setFailing(true);

        outboxPublisher.publish(inMemory);

        assertThat(outboxMapper.selectById(stored.getId()).getStatus()).isEqualTo(OutboxStatusEnum.DEAD.getStatus());
        DeadLetterDO deadLetter = deadLetterMapper.selectList().get(0);
        assertThat(deadLetter.getMessageId()).isEqualTo(inMemory.getMessageId().substring(0, DeadLetterDO.MESSAGE_ID_MAX_LENGTH));
        assertThat(deadLetter.getTopic()).hasSize(DeadLetterDO.TOPIC_MAX_LENGTH);
    }

    // ========== 不变量二：消费幂等 ==========

    @Test
    @DisplayName("首次消费应执行业务动作")
    void firstConsume_shouldExecuteAction() {
        AtomicInteger counter = new AtomicInteger();
        idempotentConsumer.consumeOnce("MSG-1", "stock-service", counter::incrementAndGet);
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("相同消息与消费者重复投递时，业务动作只执行一次")
    void duplicateDelivery_shouldExecuteActionOnlyOnce() {
        AtomicInteger counter = new AtomicInteger();
        idempotentConsumer.consumeOnce("MSG-2", "stock-service", counter::incrementAndGet);
        idempotentConsumer.consumeOnce("MSG-2", "stock-service", counter::incrementAndGet);
        idempotentConsumer.consumeOnce("MSG-2", "stock-service", counter::incrementAndGet);

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("同一消息的不同消费者应各自执行一次——一条消息常有多个下游关心")
    void differentConsumers_shouldEachExecuteOnce() {
        AtomicInteger stock = new AtomicInteger();
        AtomicInteger point = new AtomicInteger();
        idempotentConsumer.consumeOnce("MSG-3", "stock-service", stock::incrementAndGet);
        idempotentConsumer.consumeOnce("MSG-3", "point-service", point::incrementAndGet);

        assertThat(stock.get()).isEqualTo(1);
        assertThat(point.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("业务动作抛异常时消费日志必须回滚，否则这条消息永远重试不了")
    void actionFailure_shouldRollbackConsumeLogSoRetryIsPossible() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> idempotentConsumer.consumeOnce("MSG-4", "stock-service", () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("下游暂时不可用");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM infra_message_consume_log WHERE message_id = 'MSG-4'",
                Integer.class)).isZero();

        // 重试应能再次进入业务动作
        idempotentConsumer.consumeOnce("MSG-4", "stock-service", attempts::incrementAndGet);
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Mapper
    public interface DemoMapper {
    }
}
