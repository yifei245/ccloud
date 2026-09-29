package com.ccloud.module.system.service.mail;

import com.ccloud.module.system.dal.dataobject.mail.MailAccountDO;
import com.ccloud.module.system.framework.mail.MailSenderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class MailSenderFactoryTest {

    @Test
    void createMailSender_按账号配置SMTP并设置超时() {
        MailAccountDO account = new MailAccountDO().setMail("a@ccloud.local").setUsername("user").setPassword("pwd")
                .setHost("smtp.ccloud.local").setPort(465).setSslEnable(true).setStarttlsEnable(false);

        JavaMailSenderImpl sender = (JavaMailSenderImpl) new MailSenderFactory().createMailSender(account);

        assertThat(sender.getHost()).isEqualTo("smtp.ccloud.local");
        assertThat(sender.getPort()).isEqualTo(465);
        assertThat(sender.getUsername()).isEqualTo("user");
        assertThat(sender.getPassword()).isEqualTo("pwd");
        Properties props = sender.getJavaMailProperties();
        assertThat(props.getProperty("mail.smtp.auth")).isEqualTo("true");
        assertThat(props.getProperty("mail.smtp.ssl.enable")).isEqualTo("true");
        assertThat(props.getProperty("mail.smtp.starttls.enable")).isEqualTo("false");
        // 没有超时时 SMTP 服务器无响应会让消费线程永久挂起
        assertThat(props.getProperty("mail.smtp.connectiontimeout")).isNotBlank();
        assertThat(props.getProperty("mail.smtp.timeout")).isNotBlank();
        assertThat(props.getProperty("mail.smtp.writetimeout")).isNotBlank();
    }
}
