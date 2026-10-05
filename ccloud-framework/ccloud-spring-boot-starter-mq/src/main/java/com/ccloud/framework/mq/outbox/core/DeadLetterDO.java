package com.ccloud.framework.mq.outbox.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}
