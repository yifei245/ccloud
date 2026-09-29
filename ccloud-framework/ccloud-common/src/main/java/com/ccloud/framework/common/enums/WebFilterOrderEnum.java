package com.ccloud.framework.common.enums;

/**
 * Web 过滤器执行顺序。集中定义，避免各 starter 各自为政导致顺序错乱。
 *
 * <p>数值越小越先执行。三条约束决定了下面的排布：
 * <ol>
 *   <li>请求体缓存必须最先——后续的访问日志、签名校验都要重复读 body；</li>
 *   <li>登录身份解析必须早于 Spring Security 过滤器链（其默认 order 为 -100），
 *       否则鉴权时拿不到身份；</li>
 *   <li>租户安全校验必须晚于 Spring Security，因为它要比对已登录用户的租户。</li>
 * </ol>
 */
public interface WebFilterOrderEnum {

    /**
     * 请求体缓存，必须最先执行
     */
    int REQUEST_BODY_CACHE_FILTER = Integer.MIN_VALUE + 500;

    /**
     * 链路追踪，为后续所有日志提供 TraceId
     */
    int TRACE_FILTER = -110;

    /**
     * 租户上下文写入
     */
    int TENANT_CONTEXT_FILTER = -109;

    /**
     * API 访问日志
     */
    int API_ACCESS_LOG_FILTER = -108;

    /**
     * XSS 过滤
     */
    int XSS_FILTER = -107;

    /**
     * 登录身份解析。必须小于 -100，即早于 Spring Security 过滤器链
     */
    int LOGIN_USER_FILTER = -106;

    /**
     * Spring Security 过滤器链在 -100 执行（spring.security.filter.order 默认值）
     */
    int SPRING_SECURITY_FILTER = -100;

    /**
     * 租户安全校验。必须大于 -100，即晚于 Spring Security，此时才有登录用户
     */
    int TENANT_SECURITY_FILTER = -99;

    /**
     * 演示模式拦截，最后执行
     */
    int DEMO_FILTER = Integer.MAX_VALUE;
}
