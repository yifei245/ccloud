package com.ccloud.module.infra.service.mq;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mq.outbox.config.CcloudOutboxAutoConfiguration;
import com.ccloud.framework.mq.outbox.core.DeadLetterDO;
import com.ccloud.framework.mq.outbox.core.DeadLetterMapper;
import com.ccloud.framework.mq.outbox.core.MessageSender;
import com.ccloud.module.infra.controller.admin.mq.vo.MessageDeadLetterPageReqVO;
import com.ccloud.module.infra.framework.tenant.core.SystemTenantValidator;
import com.ccloud.module.infra.test.BaseInfraDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.MESSAGE_DEAD_LETTER_HANDLED;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.ONLY_SYSTEM_TENANT;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.MESSAGE_DEAD_LETTER_NOT_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Import(MessageDeadLetterServiceImpl.class)
@ImportAutoConfiguration(CcloudOutboxAutoConfiguration.class)
class MessageDeadLetterServiceImplTest extends BaseInfraDbAndRedisUnitTest {

    @Resource
    private MessageDeadLetterServiceImpl deadLetterService;
    @Resource
    private DeadLetterMapper deadLetterMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private MessageSender messageSender;
    @MockitoBean
    private SystemTenantValidator systemTenantValidator;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM infra_message_dead_letter WHERE message_id LIKE 'ut-dl-%'");
    }

    @Test
    void getDeadLetterPage_跨租户可见() {
        // 当前上下文租户为 1，死信属于租户 2 与 0
        insert("ut-dl-t2", "ut.topic", 2L);
        insert("ut-dl-t0", "ut.topic", 0L);

        MessageDeadLetterPageReqVO req = new MessageDeadLetterPageReqVO();
        req.setTopic("ut.topic");
        PageResult<DeadLetterDO> page = deadLetterService.getDeadLetterPage(req);

        assertThat(page.getList()).extracting(DeadLetterDO::getMessageId).containsExactlyInAnyOrder("ut-dl-t2", "ut-dl-t0");
        assertThat(page.getList()).extracting(DeadLetterDO::getTenantId).containsExactlyInAnyOrder(2L, 0L);
    }

    @Test
    void getDeadLetterPage_按主题消息编号与处理状态筛选() {
        insert("ut-dl-a", "ut.topic.a", 1L);
        Long handled = insert("ut-dl-b", "ut.topic.b", 1L);
        deadLetterService.updateDeadLetterHandled(handled);

        MessageDeadLetterPageReqVO req = new MessageDeadLetterPageReqVO();
        req.setTopic("ut.topic.b");
        assertThat(deadLetterService.getDeadLetterPage(req).getList()).extracting(DeadLetterDO::getMessageId).containsExactly("ut-dl-b");

        req.setTopic(null);
        req.setMessageId("ut-dl-a");
        assertThat(deadLetterService.getDeadLetterPage(req).getTotal()).isEqualTo(1);

        req.setMessageId(null);
        req.setTopic("ut.topic.");
        req.setHandled(false);
        assertThat(deadLetterService.getDeadLetterPage(req).getList()).extracting(DeadLetterDO::getMessageId).containsExactly("ut-dl-a");
    }

    @Test
    void updateDeadLetterHandled_标记已处理并记录时间_不能重复处理() {
        Long id = insert("ut-dl-h", "ut.topic", 2L);

        deadLetterService.updateDeadLetterHandled(id);

        DeadLetterDO db = deadLetterMapper.selectById(id);
        assertThat(db.getHandled()).isTrue();
        assertThat(db.getHandledTime()).isNotNull();
        assertServiceException(() -> deadLetterService.updateDeadLetterHandled(id), MESSAGE_DEAD_LETTER_HANDLED);
    }

    @Test
    void updateDeadLetterHandled_不存在报错() {
        assertServiceException(() -> deadLetterService.updateDeadLetterHandled(-1L), MESSAGE_DEAD_LETTER_NOT_EXISTS);
    }

    @Test
    void resendDeadLetter_用原消息编号发回原主题并标记已处理() {
        Long id = insert("ut-dl-r", "t.topic", 2L);

        deadLetterService.resendDeadLetter(id);

        verify(messageSender).send("t.topic", "ut-dl-r", "{\"a\":1}");
        DeadLetterDO db = deadLetterMapper.selectById(id);
        assertThat(db.getHandled()).isTrue();
        assertThat(db.getHandledTime()).isNotNull();
        assertServiceException(() -> deadLetterService.resendDeadLetter(id), MESSAGE_DEAD_LETTER_HANDLED);
    }

    @Test
    void resendDeadLetter_发送失败时保持未处理() {
        Long id = insert("ut-dl-f", "t.topic", 2L);
        doThrow(new IllegalStateException("redis 不可用")).when(messageSender).send("t.topic", "ut-dl-f", "{\"a\":1}");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> deadLetterService.resendDeadLetter(id))
                .isInstanceOf(IllegalStateException.class);

        DeadLetterDO db = deadLetterMapper.selectById(id);
        assertThat(db.getHandled()).isFalse();
        assertThat(db.getHandledTime()).isNull();
    }

    @Test
    void resendDeadLetter_不存在报错() {
        assertServiceException(() -> deadLetterService.resendDeadLetter(-1L), MESSAGE_DEAD_LETTER_NOT_EXISTS);
    }

    @Test
    void 非系统租户不能查看处理或重投死信() {
        Long id = insert("ut-dl-s", "t.topic", 2L);
        doThrow(exception(ONLY_SYSTEM_TENANT)).when(systemTenantValidator).validate();

        assertServiceException(() -> deadLetterService.getDeadLetterPage(new MessageDeadLetterPageReqVO()), ONLY_SYSTEM_TENANT);
        assertServiceException(() -> deadLetterService.updateDeadLetterHandled(id), ONLY_SYSTEM_TENANT);
        assertServiceException(() -> deadLetterService.resendDeadLetter(id), ONLY_SYSTEM_TENANT);
        verify(messageSender, never()).send(any(), any(), any());
        assertThat(deadLetterMapper.selectById(id).getHandled()).isFalse();
    }

    /**
     * 经 Mapper 写入：同时验证写入的租户取自实体、不被当前上下文（租户 1）覆盖
     */
    private Long insert(String messageId, String topic, Long tenantId) {
        DeadLetterDO deadLetter = new DeadLetterDO();
        deadLetter.setMessageId(messageId);
        deadLetter.setTopic(topic);
        deadLetter.setPayload("{\"a\":1}");
        deadLetter.setRetryCount(5);
        deadLetter.setErrorMsg("err");
        deadLetter.setHandled(false);
        deadLetter.setTenantId(tenantId);
        deadLetterMapper.insert(deadLetter);
        assertThat(jdbcTemplate.queryForObject("SELECT tenant_id FROM infra_message_dead_letter WHERE id = ?",
                Long.class, deadLetter.getId())).isEqualTo(tenantId);
        return deadLetter.getId();
    }
}
