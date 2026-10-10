package com.ccloud.framework.web.core.util;

import com.ccloud.framework.common.enums.RpcConstants;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.web.config.WebProperties;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Web 层通用工具。
 *
 * <p>登录用户信息由 security starter 写入 request attribute，本类只负责读取，
 * 从而让 web 层不反向依赖 security 层。
 */
public class WebFrameworkUtils {

    public static final String REQUEST_ATTRIBUTE_LOGIN_USER_ID = "ccloud.login_user_id";
    public static final String REQUEST_ATTRIBUTE_LOGIN_USER_TYPE = "ccloud.login_user_type";
    public static final String REQUEST_ATTRIBUTE_COMMON_RESULT = "ccloud.common_result";

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private static WebProperties properties;

    public static void setProperties(WebProperties properties) {
        WebFrameworkUtils.properties = properties;
    }

    public static void setLoginUserId(ServletRequest request, Long userId) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID, userId);
    }

    public static void setLoginUserType(ServletRequest request, Integer userType) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TYPE, userType);
    }

    public static Long getLoginUserId(HttpServletRequest request) {
        return request == null ? null : (Long) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID);
    }

    public static Long getLoginUserId() {
        return getLoginUserId(getRequest());
    }

    public static Integer getLoginUserType(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Integer userType = (Integer) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TYPE);
        if (userType != null) {
            return userType;
        }
        // 未显式设置时，按接口路径前缀推断：管理端与用户端的路径是分开的
        return getUserTypeByUrl(request.getRequestURI());
    }

    public static Integer getLoginUserType() {
        return getLoginUserType(getRequest());
    }

    /**
     * 依据请求路径推断用户类型。这是"未登录接口也要知道该查哪套用户体系"的依据，
     * 例如短信验证码登录时还没有身份，但必须知道是发给会员还是管理员。
     */
    public static Integer getUserTypeByUrl(String uri) {
        if (uri == null) {
            return null;
        }
        String adminPrefix = properties != null ? properties.getAdminApi().getPrefix() : RpcConstants.ADMIN_API_PREFIX;
        String appPrefix = properties != null ? properties.getAppApi().getPrefix() : RpcConstants.APP_API_PREFIX;
        String openPrefix = properties != null ? properties.getOpenApi().getPrefix() : RpcConstants.OPEN_API_PREFIX;
        if (uri.startsWith(adminPrefix)) {
            return UserTypeEnum.ADMIN.getValue();
        }
        if (uri.startsWith(appPrefix)) {
            return UserTypeEnum.MEMBER.getValue();
        }
        if (uri.startsWith(openPrefix)) {
            return UserTypeEnum.OPEN_APP.getValue();
        }
        return null;
    }

    public static Long getTenantId(HttpServletRequest request) {
        String value = request == null ? null : request.getHeader(HEADER_TENANT_ID);
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    /**
     * 暂存响应结果，供访问日志过滤器读取真实的业务错误码
     */
    public static void setCommonResult(ServletRequest request, CommonResult<?> result) {
        request.setAttribute(REQUEST_ATTRIBUTE_COMMON_RESULT, result);
    }

    public static CommonResult<?> getCommonResult(ServletRequest request) {
        return (CommonResult<?>) request.getAttribute(REQUEST_ATTRIBUTE_COMMON_RESULT);
    }

    public static HttpServletRequest getRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest();
    }
}
