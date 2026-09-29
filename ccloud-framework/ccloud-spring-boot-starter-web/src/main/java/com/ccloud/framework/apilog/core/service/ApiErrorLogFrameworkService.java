package com.ccloud.framework.apilog.core.service;

/**
 * API 异常日志的落库契约，实现同 {@link ApiAccessLogFrameworkService}
 */
public interface ApiErrorLogFrameworkService {

    void createApiErrorLogAsync(ApiErrorLog apiErrorLog);
}
