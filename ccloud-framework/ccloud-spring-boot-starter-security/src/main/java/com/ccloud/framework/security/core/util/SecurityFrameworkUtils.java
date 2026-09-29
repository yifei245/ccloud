package com.ccloud.framework.security.core.util;

import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

/**
 * 安全上下文读写工具
 */
public class SecurityFrameworkUtils {

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getPrincipal() instanceof LoginUser loginUser ? loginUser : null;
    }

    public static Long getLoginUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getId() : null;
    }

    public static Integer getLoginUserType() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserType() : null;
    }

    public static Long getLoginUserTenantId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getTenantId() : null;
    }

    /**
     * 写入登录身份。同时把用户编号与类型写进 request attribute，
     * 让不依赖 security 的组件（访问日志、租户校验）也能读到。
     *
     * @param request 可为 null，用于非 Web 场景
     */
    public static void setLoginUser(LoginUser loginUser, HttpServletRequest request) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        if (request != null) {
            WebFrameworkUtils.setLoginUserId(request, loginUser.getId());
            WebFrameworkUtils.setLoginUserType(request, loginUser.getUserType());
        }
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
