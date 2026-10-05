package com.ccloud.module.infra.job.logger;

import com.ccloud.module.infra.framework.config.InfraProperties;
import com.ccloud.module.infra.service.logger.ApiAccessLogService;
import com.ccloud.module.infra.service.logger.ApiErrorLogService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 过期 API 日志的清理任务，需在 XXL-Job 为本服务登记（每天凌晨执行）
 */
@Component
public class ApiLogCleanJob {

    static final int BATCH_SIZE = 10_000;

    @Resource
    private ApiAccessLogService apiAccessLogService;
    @Resource
    private ApiErrorLogService apiErrorLogService;
    @Resource
    private InfraProperties infraProperties;

    /**
     * 日志按租户隔离，清理覆盖全部租户（服务内部已忽略租户）
     */
    @XxlJob("apiAccessLogCleanJob")
    public void cleanAccessLog() {
        int days = infraProperties.getAccessRetainDays();
        int count = apiAccessLogService.cleanApiAccessLog(days, BATCH_SIZE);
        XxlJobHelper.handleSuccess(String.format("删除 %d 天前的访问日志 %d 条", days, count));
    }

    @XxlJob("apiErrorLogCleanJob")
    public void cleanErrorLog() {
        int days = infraProperties.getErrorRetainDays();
        int count = apiErrorLogService.cleanApiErrorLog(days, BATCH_SIZE);
        XxlJobHelper.handleSuccess(String.format("删除 %d 天前的异常日志 %d 条", days, count));
    }
}
