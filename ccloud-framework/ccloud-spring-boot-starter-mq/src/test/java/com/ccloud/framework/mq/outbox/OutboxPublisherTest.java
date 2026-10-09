package com.ccloud.framework.mq.outbox;

import com.ccloud.framework.mq.outbox.core.DeadLetterDO;
import com.ccloud.framework.mq.outbox.core.DeadLetterMapper;
import com.ccloud.framework.mq.outbox.core.OutboxMessageDO;
import com.ccloud.framework.mq.outbox.core.OutboxMessageMapper;
import com.ccloud.framework.mq.outbox.core.OutboxPublisher;
import com.ccloud.framework.mq.outbox.core.OutboxStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxPublisherTest {

    @Test
    void 死信写入失败时不转死信_仍按退避重试_publish不抛异常() {
        StubMessageSender sender = new StubMessageSender();
        sender.setFailing(true);
        OutboxMessageMapper outboxMapper = mock(OutboxMessageMapper.class);
        DeadLetterMapper deadLetterMapper = mock(DeadLetterMapper.class);
        doThrow(new IllegalStateException("库不可用")).when(deadLetterMapper).insert(any(DeadLetterDO.class));
        OutboxMessageDO message = new OutboxMessageDO();
        message.setId(1L);
        message.setMessageId("m-1");
        message.setTopic("t.topic");
        message.setPayload("{}");
        message.setTenantId(3L);
        message.setRetryCount(4);
        message.setMaxRetry(5);

        new OutboxPublisher(sender, outboxMapper, deadLetterMapper).publish(message);

        ArgumentCaptor<OutboxMessageDO> captor = ArgumentCaptor.forClass(OutboxMessageDO.class);
        verify(outboxMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatusEnum.FAILURE.getStatus());
        assertThat(captor.getValue().getNextRetryTime()).isNotNull();
        assertThat(captor.getValue().getRetryCount()).isEqualTo(5);
    }
}
