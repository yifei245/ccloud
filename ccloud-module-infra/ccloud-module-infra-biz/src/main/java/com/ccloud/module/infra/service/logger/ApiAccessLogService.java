package com.ccloud.module.infra.service.logger;

import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiAccessLogPageReqVO;
import com.ccloud.module.infra.dal.dataobject.logger.ApiAccessLogDO;

/**
 * API 访问日志
 */
public interface ApiAccessLogService {

    /**
     * 写入一条访问日志，按日志自带的租户编号归属
     */
    void createApiAccessLog(ApiAccessLog accessLog);

    PageResult<ApiAccessLogDO> getApiAccessLogPage(ApiAccessLogPageReqVO reqVO);

    /**
     * 跨租户物理删除保留期之前的日志
     *
     * @param retainDays 保留天数
     * @param batchSize  每批删除的条数，避免一条 SQL 锁表过久
     * @return 删除的条数
     */
    int cleanApiAccessLog(int retainDays, int batchSize);
}
