package com.ccloud.framework.security.core.filter;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.security.config.SecurityProperties;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.util.InternalSignUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 解析网关下发的内部身份头。
 *
 * <p>核心约定：<b>校验失败只意味着"没有身份"，绝不抛异常</b>。
 * 把伪造的头当成异常返回 500，等于告诉攻击者"这个头被处理了"；
 * 静默降级为匿名后，请求会正常走到鉴权环节拿到 401，行为与完全不带头一致。
 */
@Slf4j
@RequiredArgsConstructor
public class LoginUserFilter extends OncePerRequestFilter {

    public static final String HEADER_LOGIN_USER = "X-Login-User";
    public static final String HEADER_LOGIN_USER_SIGN = "X-Login-User-Sign";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";

    /**
     * 登录用户所属租户，供 TenantSecurityWebFilter 与请求声明的租户比对。
     * 通过 request attribute 传递，使租户模块不必反向依赖安全模块。
     */
    public static final String REQUEST_ATTRIBUTE_LOGIN_USER_TENANT_ID = "ccloud.login_user_tenant_id";

    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LoginUser loginUser = parseLoginUser(request);
        if (loginUser != null) {
            SecurityFrameworkUtils.setLoginUser(loginUser, request);
            request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TENANT_ID, loginUser.getTenantId());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            // 容器线程会被复用，不清理会让下一个请求继承上一个用户的身份
            SecurityContextHolder.clearContext();
        }
    }

    private LoginUser parseLoginUser(HttpServletRequest request) {
        String payload = request.getHeader(HEADER_LOGIN_USER);
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        String sign = request.getHeader(HEADER_LOGIN_USER_SIGN);
        String timestampText = request.getHeader(HEADER_TIMESTAMP);
        if (sign == null || timestampText == null) {
            log.warn("[parseLoginUser] 身份头缺少签名或时间戳，已忽略。url={}", request.getRequestURI());
            return null;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampText);
        } catch (NumberFormatException e) {
            return null;
        }

        String secret = securityProperties.getInternalSignSecret();
        if (secret == null || secret.isEmpty()) {
            // 没配密钥就无法验签。此时必须拒绝身份，否则等于完全不校验
            log.error("[parseLoginUser] 未配置 ccloud.security.internal-sign-secret，所有内部身份头将被忽略");
            return null;
        }
        if (!InternalSignUtils.verify(payload, timestamp, sign, secret,
                securityProperties.getInternalSignTolerance())) {
            log.warn("[parseLoginUser] 身份头验签失败，已忽略。url={} ip={}",
                    request.getRequestURI(), request.getRemoteAddr());
            return null;
        }

        try {
            String json = new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
            LoginUser loginUser = JsonUtils.parseObject(json, LoginUser.class);
            if (loginUser == null || loginUser.getId() == null) {
                return null;
            }
            // 二次校验过期时间：网关侧有缓存，被吊销的令牌可能在缓存期内仍被放行
            if (loginUser.isExpired()) {
                log.debug("[parseLoginUser] 身份已过期，已忽略。userId={}", loginUser.getId());
                return null;
            }
            // 管理员与会员是两套编号会重复的用户体系：管理员令牌访问 /app-api 若被接受，
            // 就会以同编号会员的身份读写数据，反之亦然。开放应用身份只在 /open-api 下有效
            Integer expectedUserType = WebFrameworkUtils.getUserTypeByUrl(request.getRequestURI());
            boolean openApp = UserTypeEnum.OPEN_APP.getValue().equals(loginUser.getUserType());
            if ((expectedUserType != null && !expectedUserType.equals(loginUser.getUserType()))
                    || (expectedUserType == null && openApp)) {
                log.warn("[parseLoginUser] 身份的用户类型({})与接口({})不符，已忽略。userId={}",
                        loginUser.getUserType(), request.getRequestURI(), loginUser.getId());
                return null;
            }
            return loginUser;
        } catch (Exception e) {
            log.warn("[parseLoginUser] 身份头解析失败，已忽略。url={}", request.getRequestURI());
            return null;
        }
    }
}
