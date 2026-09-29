package com.ccloud.framework.apilog.core.service;

import com.ccloud.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 打日志的兜底实现，同 {@link LoggerApiAccessLogFrameworkServiceImpl}
 */
@Slf4j
public class LoggerApiErrorLogFrameworkServiceImpl implements ApiErrorLogFrameworkService {

    @Override
    public void createApiErrorLogAsync(ApiErrorLog apiErrorLog) {
        log.error("[createApiErrorLogAsync][异常日志 {}]", JsonUtils.toJsonString(apiErrorLog));
    }
}
