package com.ccloud.module.system.service.mail;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.mail.vo.log.MailLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailAccountDO;
import com.ccloud.module.system.dal.dataobject.mail.MailLogDO;
import com.ccloud.module.system.dal.dataobject.mail.MailTemplateDO;

import java.util.Map;

/**
 * 邮件日志 Service
 */
public interface MailLogService {

    /**
     * 发送前落库
     *
     * @param isSend 是否发送；模板关闭或账号不存在时记为「不发送」
     * @return 日志编号
     */
    Long createMailLog(Long userId, Integer userType, String toMail, MailAccountDO account, MailTemplateDO template,
                       String title, String content, Map<String, Object> templateParams, boolean isSend);

    /**
     * 回写发送结果
     *
     * @param messageId 成功时的 Message-ID
     * @param exception 失败时的异常，为 null 表示成功
     */
    void updateMailSendResult(Long id, String messageId, Exception exception);

    MailLogDO getMailLog(Long id);

    PageResult<MailLogDO> getMailLogPage(MailLogPageReqVO pageReqVO);
}
