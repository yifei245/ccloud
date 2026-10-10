package com.ccloud.module.open.job.logger;

import com.ccloud.module.open.framework.config.OpenProperties;
import com.ccloud.module.open.service.logger.OpenApiLogService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 过期调用日志的清理任务，需在 XXL-Job 为本服务登记（每天凌晨执行）。回调日志的清理在 Task 10 加入
 */
@Component
public class OpenLogCleanJob {

    static final int BATCH_SIZE = 10_000;

    @Resource
    private OpenApiLogService openApiLogService;
    @Resource
    private OpenProperties openProperties;

    @XxlJob("openApiLogCleanJob")
    public void clean() {
        int days = openProperties.getApiLog().getRetainDays();
        int count = openApiLogService.cleanApiLog(days, BATCH_SIZE);
        XxlJobHelper.handleSuccess(String.format("删除 %d 天前的调用日志 %d 条", days, count));
    }
}
