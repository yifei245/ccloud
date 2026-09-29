package com.ccloud.framework.mq.outbox.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息消费记录。
 *
 * <p>{@code (message_id, consumer)} 上的唯一索引就是幂等的全部实现——
 * 靠数据库的唯一约束而非应用层判断，才能在并发投递下真正只执行一次。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_message_consume_log")
public class ConsumeLogDO extends BaseDO {

    private Long id;

    private String messageId;

    /**
     * 消费者标识。同一条消息可能有多个下游关心，各自独立计一次
     */
    private String consumer;

    private LocalDateTime consumeTime;
}
