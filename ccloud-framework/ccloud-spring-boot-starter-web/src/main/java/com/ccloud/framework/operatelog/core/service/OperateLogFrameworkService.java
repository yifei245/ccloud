package com.ccloud.framework.operatelog.core.service;

/**
 * 操作日志落地契约：system 服务直接写库，其他服务经 RPC 交给 system，默认实现只打日志
 */
public interface OperateLogFrameworkService {

    void createOperateLog(OperateLogRecord record);
}
