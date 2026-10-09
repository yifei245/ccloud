package com.ccloud.framework.mq.outbox.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 消息死信表。超过重试上限的消息落于此，等待人工处理或重新投递。
 *
 * <p>来源有两处：本地消息表投递重试耗尽；Redis Stream 消费重投耗尽（此时没有租户上下文，租户编号为 0）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_message_dead_letter")
public class DeadLetterDO extends TenantBaseDO {

    /**
     * 列宽，与 infra_message_dead_letter 的 DDL 一致。varchar 按字符计，text 按 UTF-8 字节计
     */
    public static final int MESSAGE_ID_MAX_LENGTH = 64;
    public static final int TOPIC_MAX_LENGTH = 128;
    public static final int ERROR_MSG_MAX_LENGTH = 1000;
    public static final int PAYLOAD_MAX_BYTES = 65_535;

    private Long id;

    /**
     * 原消息编号，重新投递时沿用，消费侧据此幂等
     */
    private String messageId;

    private String topic;

    private String payload;

    private Integer retryCount;

    /**
     * 最终失败原因
     */
    private String errorMsg;

    /**
     * 是否已人工处理
     */
    private Boolean handled;

    private LocalDateTime handledTime;

    /**
     * 写库前按列宽截断。超长字段会让写库确定性失败，消息就永远转不成死信、一直占着重投队列；
     * 截断后的消息体可能不再是合法 JSON，人工处理时以原始日志为准
     *
     * @return 本对象
     */
    public DeadLetterDO truncateToColumnWidth() {
        messageId = truncateChars(messageId, MESSAGE_ID_MAX_LENGTH);
        topic = truncateChars(topic, TOPIC_MAX_LENGTH);
        errorMsg = truncateChars(errorMsg, ERROR_MSG_MAX_LENGTH);
        payload = truncateUtf8Bytes(payload, PAYLOAD_MAX_BYTES);
        return this;
    }

    private static String truncateChars(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        int end = maxLength;
        // 不把代理对拆开
        if (Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static String truncateUtf8Bytes(String value, int maxBytes) {
        // 每个 char 最多 3 字节（代理对两个 char 共 4 字节），字符数在上限的三分之一内时无需逐字计算
        if (value == null || value.length() <= maxBytes / 3
                || value.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return value;
        }
        int bytes = 0;
        int end = 0;
        while (end < value.length()) {
            int codePoint = value.codePointAt(end);
            int size = codePoint < 0x80 ? 1 : codePoint < 0x800 ? 2 : codePoint < 0x10000 ? 3 : 4;
            if (bytes + size > maxBytes) {
                break;
            }
            bytes += size;
            end += Character.charCount(codePoint);
        }
        return value.substring(0, end);
    }
}
