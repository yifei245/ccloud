package com.ccloud.framework.apilog.core.service;

import com.ccloud.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 打日志的兜底实现。infra 模块接入后会用落库实现覆盖它。
 */
@Slf4j
public class LoggerApiAccessLogFrameworkServiceImpl implements ApiAccessLogFrameworkService {

    @Override
    public void createApiAccessLogAsync(ApiAccessLog apiAccessLog) {
        log.info("[createApiAccessLogAsync][访问日志 {}]", JsonUtils.toJsonString(apiAccessLog));
    }
}
