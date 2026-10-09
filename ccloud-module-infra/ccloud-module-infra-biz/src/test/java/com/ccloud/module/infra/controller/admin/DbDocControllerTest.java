package com.ccloud.module.infra.controller.admin;

import com.ccloud.module.infra.test.BaseInfraWebIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DbDocControllerTest extends BaseInfraWebIntegrationTest {

    @Test
    void exportHtml_返回带CSP的HTML() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/infra/db-doc/export-html"), ADMIN_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/html;charset=UTF-8"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'"))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().string(containsString("infra_demo_student")));
    }

    @Test
    void exportHtml_download为true时作为附件() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/infra/db-doc/export-html").param("download", "true"), ADMIN_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    @Test
    void exportMarkdown_返回Markdown() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/infra/db-doc/export-markdown"), ADMIN_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/markdown;charset=UTF-8"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'"))
                .andExpect(content().string(containsString("infra_demo_student")));
    }

    @Test
    void 无权限返回403() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/infra/db-doc/export-html"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/infra/db-doc/export-markdown"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }
}
