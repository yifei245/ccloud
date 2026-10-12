package com.ccloud.module.open.mq.consumer.logger;

import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.mq.redis.core.AbstractRedisStreamMessageListener;
import com.ccloud.module.open.enums.logger.OpenApiLogTopics;
import com.ccloud.module.open.service.logger.OpenApiLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 消费业务服务发来的开放接口调用日志并写库
 */
@Component
public class OpenApiLogMessageListener extends AbstractRedisStreamMessageListener<ApiAccessLog> {

    @Resource
    private OpenApiLogService openApiLogService;

    public OpenApiLogMessageListener() {
        super(OpenApiLogTopics.API_LOG, ApiAccessLog.class);
    }

    /**
     * 至少一次投递：已写库但 ACK 失败时会重复一条，日志场景可以接受，不做去重
     */
    @Override
    public void onMessage(String messageId, ApiAccessLog message) {
        openApiLogService.createApiLog(message);
    }
}
