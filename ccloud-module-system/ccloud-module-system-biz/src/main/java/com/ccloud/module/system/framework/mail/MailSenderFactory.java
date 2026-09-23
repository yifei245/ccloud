package com.ccloud.module.system.framework.mail;

import com.ccloud.module.system.dal.dataobject.mail.MailAccountDO;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 按邮箱账号创建 SMTP 发送器。每次发送新建，账号修改立即生效；邮件量不大，连接无需复用
 */
@Component
public class MailSenderFactory {

    /**
     * 连接、读、写超时（毫秒）。JavaMail 默认无限等待，SMTP 服务器无响应会让消费线程永久挂起
     */
    private static final String TIMEOUT_MILLIS = "10000";

    public JavaMailSender createMailSender(MailAccountDO account) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(account.getHost());
        sender.setPort(account.getPort());
        sender.setUsername(account.getUsername());
        sender.setPassword(account.getPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties props = sender.getJavaMailProperties();
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.ssl.enable", String.valueOf(Boolean.TRUE.equals(account.getSslEnable())));
        props.setProperty("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(account.getStarttlsEnable())));
        props.setProperty("mail.smtp.connectiontimeout", TIMEOUT_MILLIS);
        props.setProperty("mail.smtp.timeout", TIMEOUT_MILLIS);
        props.setProperty("mail.smtp.writetimeout", TIMEOUT_MILLIS);
        return sender;
    }
}
