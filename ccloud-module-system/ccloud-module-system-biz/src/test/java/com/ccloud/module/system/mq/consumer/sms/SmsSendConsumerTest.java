package com.ccloud.module.system.mq.consumer.sms;

import com.ccloud.framework.common.core.KeyValue;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.module.system.mq.message.sms.SmsSendMessage;
import com.ccloud.module.system.service.sms.SmsSendService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class SmsSendConsumerTest {

    @InjectMocks
    private SmsSendConsumer consumer;
    @Mock
    private SmsSendService smsSendService;

    @Test
    void 消息经JSON往返后参数顺序不变_并在消息所属租户下处理() {
        SmsSendMessage sent = new SmsSendMessage().setLogId(1L).setTenantId(88L).setMobile("13800000000")
                .setChannelId(2L).setApiTemplateId("tpl")
                .setTemplateParams(List.of(new KeyValue<>("name", "张三"), new KeyValue<>("code", 1234)));
        // Stream 中传递的是 JSON，消费者按监听器声明的类型反序列化
        SmsSendMessage received = JsonUtils.parseObject(JsonUtils.toJsonString(sent), consumer.getMessageType());
        AtomicLong tenantInHandler = new AtomicLong();
        doAnswer(invocation -> {
            tenantInHandler.set(TenantContextHolder.getTenantId());
            return null;
        }).when(smsSendService).doSendSms(any());

        consumer.onMessage("m1", received);

        assertThat(received).isEqualTo(sent);
        assertThat(received.getTemplateParams()).extracting(KeyValue::getKey).containsExactly("name", "code");
        assertThat(tenantInHandler.get()).isEqualTo(88L);
        assertThat(TenantContextHolder.getTenantId()).as("处理完应还原租户上下文").isNull();
    }
}
