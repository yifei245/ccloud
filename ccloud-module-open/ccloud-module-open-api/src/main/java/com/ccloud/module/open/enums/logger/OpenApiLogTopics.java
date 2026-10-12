package com.ccloud.module.open.enums.logger;

/**
 * 开放接口调用日志的 Redis Stream 主题。提供开放接口的服务发送，open 消费写库
 */
public interface OpenApiLogTopics {

    /**
     * 消息体为框架的 ApiAccessLog（JSON），userId 即应用编号
     */
    String API_LOG = "open.api-log";
}
