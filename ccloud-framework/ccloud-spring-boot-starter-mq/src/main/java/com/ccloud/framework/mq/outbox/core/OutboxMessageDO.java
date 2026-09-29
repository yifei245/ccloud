package com.ccloud.framework.mq.outbox.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 本地消息表。
 *
 * <p>与业务数据同库同事务，这是整个最终一致性方案的支点：
 * 只要业务写库成功，消息就一定在表里；只要业务回滚，消息就一定不在。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_message_outbox")
public class OutboxMessageDO extends TenantBaseDO {

    private Long id;

    /**
     * 全局唯一的消息编号，消费侧据此去重
     */
    private String messageId;

    private String topic;

    private String payload;

    /**
     * 见 {@link OutboxStatusEnum}
     */
    private Integer status;

    private Integer retryCount;

    private Integer maxRetry;

    /**
     * 下次重试时间。按指数退避计算，避免下游故障期间被重试打垮
     */
    private LocalDateTime nextRetryTime;

    private String errorMsg;
}
