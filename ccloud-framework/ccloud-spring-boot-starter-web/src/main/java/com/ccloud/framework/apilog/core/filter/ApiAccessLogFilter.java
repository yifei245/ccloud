package com.ccloud.framework.apilog.core.filter;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.apilog.core.service.ApiAccessLogFrameworkService;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.common.util.monitor.TracerUtils;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.web.config.WebProperties;
import com.ccloud.framework.web.core.filter.ApiRequestFilter;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 记录 API 访问日志。
 */
@Slf4j
public class ApiAccessLogFilter extends ApiRequestFilter {

    private final String applicationName;
    private final ApiAccessLogFrameworkService apiAccessLogFrameworkService;

    public ApiAccessLogFilter(WebProperties webProperties, String applicationName,
                              ApiAccessLogFrameworkService apiAccessLogFrameworkService) {
        super(webProperties);
        this.applicationName = applicationName;
        this.apiAccessLogFrameworkService = apiAccessLogFrameworkService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LocalDateTime beginTime = LocalDateTime.now();
        // 必须在业务执行前取参数：Controller 可能修改 request attribute
        Map<String, String> queryString = ServletUtils.getParamMap(request);
        String requestBody = ServletUtils.isJsonRequest(request) ? ServletUtils.getBody(request) : null;

        try {
            chain.doFilter(request, response);
            createApiAccessLog(request, beginTime, queryString, requestBody, null);
        } catch (Exception ex) {
            createApiAccessLog(request, beginTime, queryString, requestBody, ex);
            throw ex;
        }
    }

    private void createApiAccessLog(HttpServletRequest request, LocalDateTime beginTime,
                                    Map<String, String> queryString, String requestBody, Exception ex) {
        try {
            ApiAccessLog accessLog = new ApiAccessLog();
            accessLog.setTraceId(TracerUtils.getTraceId());
            accessLog.setUserId(WebFrameworkUtils.getLoginUserId(request));
            accessLog.setUserType(WebFrameworkUtils.getLoginUserType(request));
            accessLog.setApplicationName(applicationName);
            accessLog.setRequestMethod(request.getMethod());
            accessLog.setRequestUrl(request.getRequestURI());
            accessLog.setRequestParams(StrUtil.maxLength(JsonUtils.toJsonString(
                    MapUtil.<String, Object>builder()
                            .put("query", queryString)
                            .put("body", requestBody)
                            .build()), 8000));
            accessLog.setUserIp(ServletUtils.getClientIP(request));
            accessLog.setUserAgent(ServletUtils.getUserAgent(request));
            accessLog.setBeginTime(beginTime);
            accessLog.setEndTime(LocalDateTime.now());
            accessLog.setDuration((int) Duration.between(beginTime, accessLog.getEndTime()).toMillis());

            // 业务错误码藏在响应体里，HTTP 状态码永远是 200，只能从暂存的 CommonResult 取
            CommonResult<?> result = WebFrameworkUtils.getCommonResult(request);
            if (result != null) {
                accessLog.setResultCode(result.getCode());
                accessLog.setResultMsg(result.getMsg());
            } else if (ex != null) {
                accessLog.setResultCode(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode());
                accessLog.setResultMsg(StrUtil.maxLength(ex.getMessage(), 2000));
            } else {
                accessLog.setResultCode(GlobalErrorCodeConstants.SUCCESS.getCode());
                accessLog.setResultMsg("");
            }
            apiAccessLogFrameworkService.createApiAccessLogAsync(accessLog);
        } catch (Throwable th) {
            log.error("[createApiAccessLog] 访问日志记录失败，url={}", request.getRequestURI(), th);
        }
    }
}
