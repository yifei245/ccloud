package com.ccloud.module.system.service.logger;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.monitor.TracerUtils;
import com.ccloud.module.system.controller.admin.logger.vo.LoginLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.logger.LoginLogDO;
import com.ccloud.module.system.dal.mysql.logger.LoginLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    @Resource
    private LoginLogMapper loginLogMapper;

    /**
     * 不使用独立事务：登录流程本身不在事务中，日志插入即刻提交；
     * 登录失败时抛出的异常不会带走这条日志
     */
    @Override
    public void createLoginLog(LoginLogDO loginLog) {
        if (loginLog.getTraceId() == null) {
            loginLog.setTraceId(TracerUtils.getTraceId());
        }
        loginLogMapper.insert(loginLog);
    }

    @Override
    public PageResult<LoginLogDO> getLoginLogPage(LoginLogPageReqVO reqVO) {
        return loginLogMapper.selectPage(reqVO);
    }
}
