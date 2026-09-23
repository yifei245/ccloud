package com.ccloud.module.system.service.mail;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailAccountDO;
import com.ccloud.module.system.dal.dataobject.mail.MailLogDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.mail.MailLogMapper;
import com.ccloud.module.system.enums.mail.MailSendStatusEnum;
import com.ccloud.module.system.framework.mail.MailSenderFactory;
import com.ccloud.module.system.mq.message.mail.MailSendMessage;
import com.ccloud.module.system.service.user.AdminUserService;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Properties;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.service.mail.MailTestData.accountReqVO;
import static com.ccloud.module.system.service.mail.MailTestData.templateReqVO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Import({MailSendServiceImpl.class, MailTemplateServiceImpl.class, MailAccountServiceImpl.class, MailLogServiceImpl.class})
class MailSendServiceImplTest extends BaseSystemDbUnitTest {

    private static final String TO = "user@ccloud.local";

    @Resource
    private MailSendService mailSendService;
    @Resource
    private MailTemplateService templateService;
    @Resource
    private MailAccountService accountService;
    @Resource
    private MailLogMapper mailLogMapper;

    @MockitoBean
    private OutboxMessageService outboxMessageService;
    @MockitoBean
    private MailSenderFactory mailSenderFactory;
    @MockitoBean
    private AdminUserService adminUserService;

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    private Long accountId;

    @BeforeEach
    void seed() {
        accountId = accountService.createMailAccount(accountReqVO("noreply@ccloud.local"));
        templateService.createMailTemplate(templateReqVO(accountId, "t-ship", "订单 {orderNo} 已发货",
                "<p>{name}，您的订单 {orderNo} 已发货</p>"));
        when(mailSenderFactory.createMailSender(any())).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
    }

    // ========== 发送：落日志并发消息 ==========

    @Test
    void sendSingleMail_成功时日志为初始化并投递消息() {
        Long logId = mailSendService.sendSingleMail(TO, 7L, UserTypeEnum.MEMBER.getValue(), "t-ship",
                Map.of("name", "张三", "orderNo", "A001"));

        MailLogDO log = mailLogMapper.selectById(logId);
        assertThat(log.getSendStatus()).isEqualTo(MailSendStatusEnum.INIT.getStatus());
        assertThat(log.getFromMail()).isEqualTo("noreply@ccloud.local");
        assertThat(log.getTemplateTitle()).isEqualTo("订单 A001 已发货");
        assertThat(log.getTemplateContent()).isEqualTo("<p>张三，您的订单 A001 已发货</p>");
        assertThat(log.getTenantId()).isEqualTo(TENANT_ID);

        ArgumentCaptor<MailSendMessage> captor = ArgumentCaptor.forClass(MailSendMessage.class);
        verify(outboxMessageService).saveAndPublish(eq(MailSendMessage.TOPIC), captor.capture());
        assertThat(captor.getValue().getLogId()).isEqualTo(logId);
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void sendSingleMail_模板不存在应失败() {
        assertServiceException(() -> mailSendService.sendSingleMail(TO, null, null, "none", Map.of()),
                MAIL_TEMPLATE_NOT_EXISTS);
    }

    @Test
    void sendSingleMail_模板参数缺失应失败且不落日志() {
        assertServiceException(() -> mailSendService.sendSingleMail(TO, null, null, "t-ship", Map.of("name", "张三")),
                MAIL_SEND_TEMPLATE_PARAM_MISS);
        assertThat(mailLogMapper.selectList()).isEmpty();
    }

    @Test
    void sendSingleMail_邮箱为空应失败() {
        assertServiceException(() -> mailSendService.sendSingleMail(null, null, null, "t-ship", Map.of()),
                MAIL_SEND_MAIL_NOT_EXISTS);
    }

    @Test
    void sendSingleMail_模板关闭时记为不发送且不投递() {
        MailTemplateSaveReqVO reqVO = templateReqVO(accountId, "t-off", "x", "y");
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        templateService.createMailTemplate(reqVO);

        Long logId = mailSendService.sendSingleMail(TO, null, null, "t-off", Map.of());

        assertThat(mailLogMapper.selectById(logId).getSendStatus()).isEqualTo(MailSendStatusEnum.IGNORE.getStatus());
        verifyNoInteractions(outboxMessageService);
    }

    @Test
    void sendSingleMailToAdmin_未传邮箱时取管理员邮箱() {
        when(adminUserService.getUser(9L)).thenReturn(new AdminUserDO().setId(9L).setEmail("admin@ccloud.local"));

        Long logId = mailSendService.sendSingleMailToAdmin(null, 9L, "t-ship", Map.of("name", "a", "orderNo", "b"));

        MailLogDO log = mailLogMapper.selectById(logId);
        assertThat(log.getToMail()).isEqualTo("admin@ccloud.local");
        assertThat(log.getUserType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
    }

    // ========== 消费：SMTP 发送并回写结果 ==========

    @Test
    void doSendMail_成功时按账号发件并回写消息编号() throws Exception {
        Long logId = mailSendService.sendSingleMail(TO, null, null, "t-ship", Map.of("name", "张三", "orderNo", "A001"));

        mailSendService.doSendMail(message(logId));

        ArgumentCaptor<MailAccountDO> account = ArgumentCaptor.forClass(MailAccountDO.class);
        verify(mailSenderFactory).createMailSender(account.capture());
        assertThat(account.getValue().getPassword()).isEqualTo("smtp-password");
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage mime = sent.getValue();
        assertThat(mime.getSubject()).isEqualTo("订单 A001 已发货");
        assertThat(mime.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(TO);
        assertThat(mime.getFrom()[0].toString()).contains("noreply@ccloud.local");

        MailLogDO log = mailLogMapper.selectById(logId);
        assertThat(log.getSendStatus()).isEqualTo(MailSendStatusEnum.SUCCESS.getStatus());
        assertThat(log.getSendTime()).isNotNull();
    }

    @Test
    void doSendMail_发送异常时日志记为失败并保留异常信息() {
        Long logId = mailSendService.sendSingleMail(TO, null, null, "t-ship", Map.of("name", "张三", "orderNo", "A001"));
        doThrow(new MailSendException("535 Authentication failed")).when(mailSender).send(any(MimeMessage.class));

        mailSendService.doSendMail(message(logId));

        MailLogDO log = mailLogMapper.selectById(logId);
        assertThat(log.getSendStatus()).isEqualTo(MailSendStatusEnum.FAILURE.getStatus());
        assertThat(log.getSendException()).contains("535 Authentication failed");
    }

    @Test
    void doSendMail_日志已处理过时跳过() {
        Long logId = mailSendService.sendSingleMail(TO, null, null, "t-ship", Map.of("name", "张三", "orderNo", "A001"));

        mailSendService.doSendMail(message(logId));
        mailSendService.doSendMail(message(logId));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    private MailSendMessage message(Long logId) {
        return new MailSendMessage().setLogId(logId).setTenantId(TENANT_ID);
    }
}
