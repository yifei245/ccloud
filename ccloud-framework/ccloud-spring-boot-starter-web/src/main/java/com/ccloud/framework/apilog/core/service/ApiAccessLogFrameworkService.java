package com.ccloud.framework.apilog.core.service;

/**
 * API 访问日志的落库契约。
 *
 * <p>框架层只定义"要记什么"，"记到哪"由 infra 模块实现（P2）。
 * P1 阶段由 {@link LoggerApiAccessLogFrameworkServiceImpl} 打日志兜底，
 * 保证不引入 infra 依赖也能跑通。
 */
public interface ApiAccessLogFrameworkService {

    /**
     * 异步创建访问日志。实现方必须自行保证不阻塞业务线程。
     */
    void createApiAccessLogAsync(ApiAccessLog apiAccessLog);
}
