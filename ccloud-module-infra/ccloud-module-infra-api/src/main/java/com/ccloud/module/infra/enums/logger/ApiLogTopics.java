package com.ccloud.module.infra.enums.logger;

/**
 * API 日志的 Redis Stream 主题。system、member 等服务发送，infra 消费并写库
 */
public interface ApiLogTopics {

    /**
     * 消息体为框架的 ApiAccessLog（JSON）
     */
    String ACCESS_LOG = "infra.api-access-log";
    /**
     * 消息体为框架的 ApiErrorLog（JSON）
     */
    String ERROR_LOG = "infra.api-error-log";
}
