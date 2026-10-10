package com.ccloud.module.open.service.logger;

import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.open.controller.admin.logger.vo.OpenApiLogPageReqVO;
import com.ccloud.module.open.dal.dataobject.logger.OpenApiLogDO;

/**
 * 开放接口调用日志
 */
public interface OpenApiLogService {

    /**
     * 写入一条调用日志：按日志自带的租户写入，应用标识与接口编码由 open 反查
     */
    void createApiLog(ApiAccessLog accessLog);

    /**
     * 当前租户的调用日志分页
     */
    PageResult<OpenApiLogDO> getApiLogPage(OpenApiLogPageReqVO reqVO);

    /**
     * 物理删除 retainDays 天前的调用日志（跨租户），按批删除；返回删除条数
     */
    int cleanApiLog(int retainDays, int batchSize);
}
