package com.ccloud.module.system.mq.consumer.mail;

import com.ccloud.framework.mq.redis.core.AbstractRedisStreamMessageListener;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.mq.message.mail.MailSendMessage;
import com.ccloud.module.system.service.mail.MailSendService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class MailSendConsumer extends AbstractRedisStreamMessageListener<MailSendMessage> {

    @Resource
    private MailSendService mailSendService;

    public MailSendConsumer() {
        super(MailSendMessage.TOPIC, MailSendMessage.class);
    }

    @Override
    public void onMessage(String messageId, MailSendMessage message) {
        TenantUtils.execute(message.getTenantId(), () -> mailSendService.doSendMail(message));
    }
}
