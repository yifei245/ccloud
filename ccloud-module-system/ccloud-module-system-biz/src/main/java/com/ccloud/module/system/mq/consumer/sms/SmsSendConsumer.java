package com.ccloud.module.system.mq.consumer.sms;

import com.ccloud.framework.mq.redis.core.AbstractRedisStreamMessageListener;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.mq.message.sms.SmsSendMessage;
import com.ccloud.module.system.service.sms.SmsSendService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SmsSendConsumer extends AbstractRedisStreamMessageListener<SmsSendMessage> {

    @Resource
    private SmsSendService smsSendService;

    public SmsSendConsumer() {
        super(SmsSendMessage.TOPIC, SmsSendMessage.class);
    }

    @Override
    public void onMessage(String messageId, SmsSendMessage message) {
        TenantUtils.execute(message.getTenantId(), () -> smsSendService.doSendSms(message));
    }
}
