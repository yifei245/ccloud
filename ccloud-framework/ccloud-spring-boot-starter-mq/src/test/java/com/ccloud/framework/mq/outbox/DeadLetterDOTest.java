package com.ccloud.framework.mq.outbox;

import com.ccloud.framework.mq.outbox.core.DeadLetterDO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 死信写库前按列宽截断：超长的消息编号、主题、失败原因或消息体会让写库确定性失败，
 * 消息因此永远转不成死信、一直占着重投队列
 */
class DeadLetterDOTest {

    @Test
    void 超长字段按列宽截断() {
        DeadLetterDO deadLetter = deadLetter("m".repeat(100), "t".repeat(200), "p".repeat(70_000), "e".repeat(2000));

        deadLetter.truncateToColumnWidth();

        assertThat(deadLetter.getMessageId()).isEqualTo("m".repeat(DeadLetterDO.MESSAGE_ID_MAX_LENGTH));
        assertThat(deadLetter.getTopic()).isEqualTo("t".repeat(DeadLetterDO.TOPIC_MAX_LENGTH));
        assertThat(deadLetter.getErrorMsg()).isEqualTo("e".repeat(DeadLetterDO.ERROR_MSG_MAX_LENGTH));
        assertThat(deadLetter.getPayload()).isEqualTo("p".repeat(DeadLetterDO.PAYLOAD_MAX_BYTES));
    }

    @Test
    void 消息体按UTF8字节数截断_不截断半个字符() {
        // text 列上限是 65535 字节而不是字符：中文每字 3 字节，emoji 4 字节（Java 中占两个 char）
        DeadLetterDO deadLetter = deadLetter("m1", "t", "中".repeat(30_000), null);
        deadLetter.truncateToColumnWidth();
        assertThat(deadLetter.getPayload()).isEqualTo("中".repeat(65_535 / 3));

        deadLetter = deadLetter("m1", "t", "a" + "😀".repeat(20_000), null);
        deadLetter.truncateToColumnWidth();
        byte[] bytes = deadLetter.getPayload().getBytes(StandardCharsets.UTF_8);
        assertThat(bytes.length).isLessThanOrEqualTo(DeadLetterDO.PAYLOAD_MAX_BYTES);
        assertThat(deadLetter.getPayload()).isEqualTo("a" + "😀".repeat((65_535 - 1) / 4));

        // varchar 按字符计：不能把代理对拆开
        deadLetter = deadLetter("a" + "😀".repeat(40), "t", null, null);
        deadLetter.truncateToColumnWidth();
        assertThat(deadLetter.getMessageId()).isEqualTo("a" + "😀".repeat(31));
    }

    @Test
    void 未超长与空值保持原样() {
        DeadLetterDO deadLetter = deadLetter("m1", "t.topic", "{\"a\":1}", null);
        deadLetter.truncateToColumnWidth();
        assertThat(deadLetter.getMessageId()).isEqualTo("m1");
        assertThat(deadLetter.getTopic()).isEqualTo("t.topic");
        assertThat(deadLetter.getPayload()).isEqualTo("{\"a\":1}");
        assertThat(deadLetter.getErrorMsg()).isNull();

        deadLetter.setPayload(null);
        deadLetter.truncateToColumnWidth();
        assertThat(deadLetter.getPayload()).isNull();
    }

    private static DeadLetterDO deadLetter(String messageId, String topic, String payload, String errorMsg) {
        DeadLetterDO deadLetter = new DeadLetterDO();
        deadLetter.setMessageId(messageId);
        deadLetter.setTopic(topic);
        deadLetter.setPayload(payload);
        deadLetter.setErrorMsg(errorMsg);
        return deadLetter;
    }
}
