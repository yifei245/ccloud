package com.ccloud.module.system.service.mail;

import com.ccloud.module.system.mq.message.mail.MailSendMessage;

import java.util.Map;

/**
 * 邮件发送 Service：同步渲染并落日志，经本地消息表与 Redis Stream 异步发送，
 * 由 {@link com.ccloud.module.system.mq.consumer.mail.MailSendConsumer} 调用 SMTP 并回写结果
 */
public interface MailSendService {

    /**
     * @param mail 为空时按 userId 取管理员邮箱
     * @return 邮件日志编号
     */
    Long sendSingleMailToAdmin(String mail, Long userId, String templateCode, Map<String, Object> templateParams);

    /**
     * @param mail 必填；按会员编号取邮箱随会员模块（Task 40）提供
     * @return 邮件日志编号
     */
    Long sendSingleMailToMember(String mail, Long userId, String templateCode, Map<String, Object> templateParams);

    /**
     * 模板关闭或邮箱账号不存在时记为「不发送」，不投递
     *
     * @return 邮件日志编号
     */
    Long sendSingleMail(String mail, Long userId, Integer userType, String templateCode,
                        Map<String, Object> templateParams);

    /**
     * 消费发送消息：按日志内容发送并回写结果。日志已不是初始化状态时跳过
     */
    void doSendMail(MailSendMessage message);
}
