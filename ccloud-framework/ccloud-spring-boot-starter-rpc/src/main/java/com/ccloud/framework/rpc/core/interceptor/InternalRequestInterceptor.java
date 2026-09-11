package com.ccloud.framework.rpc.core.interceptor;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.security.config.SecurityProperties;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.filter.LoginUserFilter;
import com.ccloud.framework.security.core.util.InternalSignUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 透传身份与租户上下文到下游服务。
 *
 * <p>签名算法必须与 {@link LoginUserFilter} 的验签严格一致——
 * 两边只要有一点出入，所有 RPC 调用都会在下游退化成匿名，
 * 表现为莫名其妙的 401，而不是一个指向签名的明确错误。
 */
@Slf4j
@RequiredArgsConstructor
public class InternalRequestInterceptor implements RequestInterceptor {

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private final SecurityProperties securityProperties;

    @Override
    public void apply(RequestTemplate template) {
        propagateTenant(template);
        propagateLoginUser(template);
    }

    /**
     * 租户上下文优先于登录用户自带的租户。
     * 系统任务用 {@code TenantUtils.execute} 切换租户时，上下文才是当前真正的租户。
     */
    private void propagateTenant(RequestTemplate template) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
            tenantId = loginUser != null ? loginUser.getTenantId() : null;
        }
        if (tenantId != null) {
            template.header(HEADER_TENANT_ID, String.valueOf(tenantId));
        }
    }

    private void propagateLoginUser(RequestTemplate template) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return;
        }
        String secret = securityProperties.getInternalSignSecret();
        if (secret == null || secret.isEmpty()) {
            // 不带密钥就别发身份头：下游必然验不过，白白让调用退化成匿名且难以定位
            log.error("[propagateLoginUser] 未配置 ccloud.security.internal-sign-secret，RPC 调用将不携带身份");
            return;
        }
        String payload = Base64.getEncoder().encodeToString(
                JsonUtils.toJsonString(loginUser).getBytes(StandardCharsets.UTF_8));
        long timestamp = System.currentTimeMillis();
        template.header(LoginUserFilter.HEADER_LOGIN_USER, payload);
        template.header(LoginUserFilter.HEADER_TIMESTAMP, String.valueOf(timestamp));
        template.header(LoginUserFilter.HEADER_LOGIN_USER_SIGN,
                InternalSignUtils.sign(payload, timestamp, secret));
    }
}
