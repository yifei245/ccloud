package com.ccloud.module.system.service.mail;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.mail.vo.log.MailLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailAccountDO;
import com.ccloud.module.system.dal.dataobject.mail.MailLogDO;
import com.ccloud.module.system.dal.dataobject.mail.MailTemplateDO;
import com.ccloud.module.system.dal.mysql.mail.MailLogMapper;
import com.ccloud.module.system.enums.mail.MailSendStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class MailLogServiceImpl implements MailLogService {

    /**
     * send_exception 列长度 4096
     */
    private static final int EXCEPTION_MAX_LENGTH = 4000;

    @Resource
    private MailLogMapper mailLogMapper;

    @Override
    public Long createMailLog(Long userId, Integer userType, String toMail, MailAccountDO account,
                              MailTemplateDO template, String title, String content,
                              Map<String, Object> templateParams, boolean isSend) {
        MailLogDO log = new MailLogDO().setUserId(userId).setUserType(userType).setToMail(toMail)
                .setAccountId(template.getAccountId()).setFromMail(account != null ? account.getMail() : "")
                .setTemplateId(template.getId()).setTemplateCode(template.getCode())
                .setTemplateNickname(template.getNickname()).setTemplateTitle(title).setTemplateContent(content)
                .setTemplateParams(templateParams)
                .setSendStatus(isSend ? MailSendStatusEnum.INIT.getStatus() : MailSendStatusEnum.IGNORE.getStatus());
        mailLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public void updateMailSendResult(Long id, String messageId, Exception exception) {
        MailLogDO update = new MailLogDO().setId(id).setSendTime(LocalDateTime.now());
        if (exception == null) {
            update.setSendStatus(MailSendStatusEnum.SUCCESS.getStatus()).setSendMessageId(messageId);
        } else {
            update.setSendStatus(MailSendStatusEnum.FAILURE.getStatus())
                    .setSendException(StrUtil.maxLength(ExceptionUtil.getRootCauseMessage(exception) + "\n"
                            + ExceptionUtil.stacktraceToString(exception), EXCEPTION_MAX_LENGTH));
        }
        mailLogMapper.updateById(update);
    }

    @Override
    public MailLogDO getMailLog(Long id) {
        return mailLogMapper.selectById(id);
    }

    @Override
    public PageResult<MailLogDO> getMailLogPage(MailLogPageReqVO pageReqVO) {
        return mailLogMapper.selectPage(pageReqVO);
    }
}
