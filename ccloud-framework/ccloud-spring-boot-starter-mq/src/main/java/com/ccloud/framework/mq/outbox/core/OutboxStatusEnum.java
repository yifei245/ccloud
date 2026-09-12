package com.ccloud.framework.mq.outbox.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 本地消息状态
 */
@Getter
@AllArgsConstructor
public enum OutboxStatusEnum {

    PENDING(0, "待投递"),
    SUCCESS(1, "投递成功"),
    FAILURE(2, "投递失败待重试"),
    DEAD(3, "超过重试上限");

    private final Integer status;
    private final String name;
}
