package com.ccloud.gateway.filter;

import com.ccloud.gateway.support.GatewayIntegrationTestSupport;
import com.ccloud.gateway.support.StubServer;
import com.sun.net.httpserver.Headers;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 外部伪造的内部身份头不能穿透到下游。这是安全底线：下游只要验签通过就完全信任这些头
 */
class RemoveInternalHeaderFilterTest extends GatewayIntegrationTestSupport {

    private static final String FORGED_PAYLOAD = Base64.getEncoder()
            .encodeToString("{\"id\":1,\"userType\":2,\"tenantId\":1}".getBytes());

    @Test
    void 外部请求携带X_Login_User头时必须被剥离() {
        webClient.post().uri("/admin-api/system/auth/login")
                .header("X-Login-User", FORGED_PAYLOAD)
                .exchange().expectStatus().isOk();

        assertThat(received().getFirst("X-Login-User")).isNull();
    }

    @Test
    void 外部请求携带X_Login_User_Sign头时必须被剥离() {
        webClient.post().uri("/admin-api/system/auth/login")
                .header("X-Login-User", FORGED_PAYLOAD)
                .header("X-Login-User-Sign", "forged-sign")
                .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                .exchange().expectStatus().isOk();

        Headers headers = received();
        assertThat(headers.getFirst("X-Login-User-Sign")).isNull();
        assertThat(headers.getFirst("X-Timestamp")).isNull();
    }

    @Test
    void 头名大小写不同也必须被剥离() {
        webClient.post().uri("/admin-api/system/auth/login")
                .header("x-login-user", FORGED_PAYLOAD)
                .header("X-LOGIN-USER-SIGN", "forged-sign")
                .exchange().expectStatus().isOk();

        Headers headers = received();
        assertThat(headers.getFirst("X-Login-User")).isNull();
        assertThat(headers.getFirst("X-Login-User-Sign")).isNull();
    }

    /**
     * 计划要求无条件剥离 X-Tenant-Id，但匿名接口（登录、发送验证码）只能靠它确定租户，
     * 剥离后下游一律返回"请求未携带租户编号"。因此改为：已登录请求以令牌租户覆盖，匿名请求保留
     */
    @Test
    void 外部请求携带X_Tenant_Id头时_已登录请求以令牌中的租户覆盖() {
        String token = registerToken(1L, 2, TENANT_ID, java.time.LocalDateTime.now().plusHours(1));

        webClient.get().uri("/admin-api/system/user/page")
                .headers(h -> h.setBearerAuth(token))
                .header("X-Tenant-Id", "999")
                .exchange().expectStatus().isOk();

        assertThat(received().get("X-Tenant-Id")).containsExactly(String.valueOf(TENANT_ID));
    }

    @Test
    void 匿名请求的X_Tenant_Id应保留_登录接口依赖它确定租户() {
        webClient.post().uri("/admin-api/system/auth/login")
                .header("X-Tenant-Id", "3")
                .exchange().expectStatus().isOk();

        assertThat(received().get("X-Tenant-Id")).containsExactly("3");
    }

    @Test
    void 携带有效令牌时伪造头被网关签发的头替换而不是并存() {
        String token = adminToken();

        webClient.get().uri("/admin-api/system/user/page")
                .headers(h -> h.setBearerAuth(token))
                .header("X-Login-User", FORGED_PAYLOAD)
                .header("X-Login-User-Sign", "forged-sign")
                .exchange().expectStatus().isOk();

        Headers headers = received();
        assertThat(headers.get("X-Login-User")).hasSize(1).doesNotContain(FORGED_PAYLOAD);
        assertThat(headers.get("X-Login-User-Sign")).hasSize(1).doesNotContain("forged-sign");
    }

    private static Headers received() {
        assertThat(SYSTEM.requests).hasSize(1);
        StubServer.Received request = SYSTEM.requests.get(0);
        return request.headers();
    }
}
