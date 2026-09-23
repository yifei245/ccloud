package com.ccloud.module.system.service.mail;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.module.system.dal.dataobject.mail.MailAccountDO;
import com.ccloud.module.system.dal.dataobject.mail.MailLogDO;
import com.ccloud.module.system.dal.dataobject.mail.MailTemplateDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.enums.mail.MailSendStatusEnum;
import com.ccloud.module.system.framework.mail.MailSenderFactory;
import com.ccloud.module.system.mq.message.mail.MailSendMessage;
import com.ccloud.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Slf4j
@Service
public class MailSendServiceImpl implements MailSendService {

    @Resource
    private MailTemplateService mailTemplateService;
    @Resource
    private MailAccountService mailAccountService;
    @Resource
    private MailLogService mailLogService;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private MailSenderFactory mailSenderFactory;
    @Resource
    private OutboxMessageService outboxMessageService;

    @Override
    public Long sendSingleMailToAdmin(String mail, Long userId, String templateCode, Map<String, Object> templateParams) {
        if (StrUtil.isEmpty(mail) && userId != null) {
            AdminUserDO user = adminUserService.getUser(userId);
            if (user != null) {
                mail = user.getEmail();
            }
        }
        return sendSingleMail(mail, userId, UserTypeEnum.ADMIN.getValue(), templateCode, templateParams);
    }

    @Override
    public Long sendSingleMailToMember(String mail, Long userId, String templateCode, Map<String, Object> templateParams) {
        return sendSingleMail(mail, userId, UserTypeEnum.MEMBER.getValue(), templateCode, templateParams);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendSingleMail(String mail, Long userId, Integer userType, String templateCode,
                               Map<String, Object> templateParams) {
        MailTemplateDO template = mailTemplateService.getMailTemplateByCode(templateCode);
        if (template == null) {
            throw exception(MAIL_TEMPLATE_NOT_EXISTS);
        }
        if (StrUtil.isEmpty(mail)) {
            throw exception(MAIL_SEND_MAIL_NOT_EXISTS);
        }
        Map<String, Object> params = templateParams == null ? Map.of() : templateParams;
        for (String key : template.getParams()) {
            if (params.get(key) == null) {
                throw exception(MAIL_SEND_TEMPLATE_PARAM_MISS, key);
            }
        }

        MailAccountDO account = mailAccountService.getMailAccount(template.getAccountId());
        boolean isSend = CommonStatusEnum.isEnable(template.getStatus()) && account != null;
        String title = mailTemplateService.formatMailTemplateTitle(template.getTitle(), params);
        String content = mailTemplateService.formatMailTemplateContent(template.getContent(), params);
        Long logId = mailLogService.createMailLog(userId, userType, mail, account, template, title, content,
                params, isSend);
        if (isSend) {
            outboxMessageService.saveAndPublish(MailSendMessage.TOPIC,
                    new MailSendMessage().setLogId(logId).setTenantId(TenantContextHolder.getTenantId()));
        }
        return logId;
    }

    @Override
    public void doSendMail(MailSendMessage message) {
        MailLogDO mailLog = mailLogService.getMailLog(message.getLogId());
        if (mailLog == null || !Objects.equals(mailLog.getSendStatus(), MailSendStatusEnum.INIT.getStatus())) {
            log.info("[doSendMail] 邮件日志 {} 不存在或已处理，跳过", message.getLogId());
            return;
        }
        MailAccountDO account = mailAccountService.getMailAccount(mailLog.getAccountId());
        if (account == null) {
            mailLogService.updateMailSendResult(mailLog.getId(), null, new IllegalStateException("邮箱账号不存在"));
            return;
        }
        try {
            JavaMailSender sender = mailSenderFactory.createMailSender(account);
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            if (StrUtil.isNotEmpty(mailLog.getTemplateNickname())) {
                helper.setFrom(account.getMail(), mailLog.getTemplateNickname());
            } else {
                helper.setFrom(account.getMail());
            }
            helper.setTo(mailLog.getToMail());
            helper.setSubject(mailLog.getTemplateTitle());
            helper.setText(mailLog.getTemplateContent(), true);
            sender.send(mimeMessage);
            mailLogService.updateMailSendResult(mailLog.getId(), mimeMessage.getMessageID(), null);
        } catch (Exception e) {
            // 记为失败而不抛出重投：认证失败、地址无效等错误重试也不会成功，且失败原因已完整记入日志
            log.error("[doSendMail] 邮件日志 {} 发送失败", mailLog.getId(), e);
            mailLogService.updateMailSendResult(mailLog.getId(), null, e);
        }
    }
}
