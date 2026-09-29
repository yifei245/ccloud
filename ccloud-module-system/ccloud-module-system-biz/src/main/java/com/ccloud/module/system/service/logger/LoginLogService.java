package com.ccloud.module.system.service.logger;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.logger.vo.LoginLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.logger.LoginLogDO;

/**
 * 登录日志 Service
 */
public interface LoginLogService {

    void createLoginLog(LoginLogDO loginLog);

    PageResult<LoginLogDO> getLoginLogPage(LoginLogPageReqVO reqVO);
}
