package com.ccloud.framework.operatelog.core.service;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认实现：只写应用日志，未接入 system 时也不影响业务
 */
@Slf4j
public class LoggerOperateLogFrameworkServiceImpl implements OperateLogFrameworkService {

    @Override
    public void createOperateLog(OperateLogRecord record) {
        log.info("[createOperateLog] {}", record);
    }
}
