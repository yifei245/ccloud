package com.ccloud.module.infra.mq.consumer.logger;

import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.mq.redis.core.AbstractRedisStreamMessageListener;
import com.ccloud.module.infra.enums.logger.ApiLogTopics;
import com.ccloud.module.infra.service.logger.ApiAccessLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 消费其他服务发来的 API 访问日志并写库
 */
@Component
public class ApiAccessLogMessageListener extends AbstractRedisStreamMessageListener<ApiAccessLog> {

    @Resource
    private ApiAccessLogService apiAccessLogService;

    public ApiAccessLogMessageListener() {
        super(ApiLogTopics.ACCESS_LOG, ApiAccessLog.class);
    }

    /**
     * 至少一次投递：已写库但 ACK 失败时会重复一条，日志场景可以接受，不做去重
     */
    @Override
    public void onMessage(String messageId, ApiAccessLog message) {
        apiAccessLogService.createApiAccessLog(message);
    }
}
