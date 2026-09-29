package com.ccloud.module.system.service.logger;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.logger.vo.OperateLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.logger.OperateLogDO;

/**
 * 操作日志 Service
 */
public interface OperateLogService {

    /**
     * 超长字段按列长度截断
     */
    void createOperateLog(OperateLogDO operateLog);

    PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqVO pageReqVO);
}
