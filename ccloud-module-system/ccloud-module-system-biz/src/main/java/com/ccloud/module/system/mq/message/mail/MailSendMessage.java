package com.ccloud.module.system.mq.message.mail;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 邮件发送消息。标题、内容等已渲染在邮件日志中，消息只携带日志编号，避免大段 HTML 进出 Redis
 */
@Data
@Accessors(chain = true)
public class MailSendMessage {

    public static final String TOPIC = "system.mail.send";

    private Long logId;
    /**
     * 邮件日志所属租户，消费线程据此切换租户上下文
     */
    private Long tenantId;
}
