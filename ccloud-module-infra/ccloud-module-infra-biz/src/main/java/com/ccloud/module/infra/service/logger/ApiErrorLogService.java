package com.ccloud.module.infra.service.logger;

import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiErrorLogPageReqVO;
import com.ccloud.module.infra.dal.dataobject.logger.ApiErrorLogDO;

/**
 * API 异常日志
 */
public interface ApiErrorLogService {

    /**
     * 写入一条异常日志，按日志自带的租户编号归属，初始状态为未处理
     */
    void createApiErrorLog(ApiErrorLog errorLog);

    PageResult<ApiErrorLogDO> getApiErrorLogPage(ApiErrorLogPageReqVO reqVO);

    /**
     * 处理异常日志
     *
     * @param processStatus 1 已处理，2 已忽略
     */
    void updateApiErrorLogProcess(Long id, Integer processStatus, Long processUserId);

    /**
     * 跨租户物理删除保留期之前的日志
     *
     * @return 删除的条数
     */
    int cleanApiErrorLog(int retainDays, int batchSize);
}
