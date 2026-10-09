package com.ccloud.module.infra.controller.admin;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.infra.test.BaseInfraWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.ccloud.module.infra.enums.ErrorCodeConstants.ONLY_SYSTEM_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 参数配置、文件配置与消息死信为全部租户共用：写操作与死信的全部接口只允许系统租户调用，
 * 即使普通租户的套餐里含这些菜单也不行
 */
class SystemTenantOnlyControllerTest extends BaseInfraWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM infra_config WHERE config_key LIKE 'webtest.%'");
        jdbcTemplate.update("DELETE FROM infra_file_config WHERE name LIKE 'Web测试%'");
    }

    @Test
    void 非系统租户_参数配置与文件配置的写操作和死信接口被拒_读取不受限() throws Exception {
        when(tenantApi.isSystemTenant(TENANT)).thenReturn(CommonResult.success(false));

        rejected(post("/admin-api/infra/config/create").contentType(MediaType.APPLICATION_JSON).content(configBody(null)));
        rejected(put("/admin-api/infra/config/update").contentType(MediaType.APPLICATION_JSON).content(configBody(1L)));
        rejected(delete("/admin-api/infra/config/delete").param("id", "1"));
        rejected(post("/admin-api/infra/file-config/create").contentType(MediaType.APPLICATION_JSON).content(fileConfigBody(null)));
        rejected(put("/admin-api/infra/file-config/update").contentType(MediaType.APPLICATION_JSON).content(fileConfigBody(1L)));
        rejected(put("/admin-api/infra/file-config/update-master").param("id", "1"));
        rejected(delete("/admin-api/infra/file-config/delete").param("id", "1"));
        rejected(get("/admin-api/infra/file-config/test").param("id", "1"));
        rejected(get("/admin-api/infra/message-dead-letter/page"));
        rejected(put("/admin-api/infra/message-dead-letter/update-handled").param("id", "1"));
        rejected(put("/admin-api/infra/message-dead-letter/resend").param("id", "1"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infra_config WHERE config_key LIKE 'webtest.%'",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infra_file_config WHERE name LIKE 'Web测试%'",
                Integer.class)).isZero();

        // 读取不受限
        mockMvc.perform(asAdmin(get("/admin-api/infra/config/page"), ADMIN_ID)).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/infra/file-config/page"), ADMIN_ID)).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/rpc-api/infra/config/get-value-by-key").param("key", "webtest.none").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void 系统租户_写操作与死信接口通过() throws Exception {
        when(tenantApi.isSystemTenant(TENANT)).thenReturn(CommonResult.success(true));

        mockMvc.perform(asAdmin(post("/admin-api/infra/config/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(configBody(null)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(post("/admin-api/infra/file-config/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(fileConfigBody(null)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/infra/message-dead-letter/page"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));
        // 业务校验照常进行：说明请求越过了系统租户校验
        mockMvc.perform(asAdmin(put("/admin-api/infra/message-dead-letter/update-handled").param("id", "-1"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_002_005_000));
    }

    private void rejected(MockHttpServletRequestBuilder builder) throws Exception {
        mockMvc.perform(asAdmin(builder, ADMIN_ID)).andExpect(jsonPath("$.code").value(ONLY_SYSTEM_TENANT.getCode()));
    }

    private static String configBody(Long id) {
        return "{" + (id != null ? "\"id\":" + id + "," : "")
                + "\"category\":\"webtest\",\"name\":\"Web测试\",\"key\":\"webtest.sys\",\"value\":\"1\",\"visible\":true}";
    }

    private static String fileConfigBody(Long id) {
        return "{" + (id != null ? "\"id\":" + id + "," : "")
                + "\"name\":\"Web测试-系统租户\",\"storage\":1,\"config\":{\"domain\":\"http://127.0.0.1:28080\"}}";
    }
}
