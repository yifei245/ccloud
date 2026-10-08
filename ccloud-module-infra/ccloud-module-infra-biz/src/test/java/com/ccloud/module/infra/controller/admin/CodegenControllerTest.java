package com.ccloud.module.infra.controller.admin;

import com.ccloud.module.infra.test.BaseInfraWebIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CodegenControllerTest extends BaseInfraWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM infra_codegen_column WHERE table_id IN "
                + "(SELECT id FROM (SELECT id FROM infra_codegen_table WHERE table_name LIKE 'infra_demo_%') t)");
        jdbcTemplate.update("DELETE FROM infra_codegen_table WHERE table_name LIKE 'infra_demo_%'");
    }

    @Test
    void 导入学生表_详情_预览与下载生成的代码() throws Exception {
        String tableId = mockMvc.perform(asAdmin(post("/admin-api/infra/codegen/create-list"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataSourceConfigId\":0,\"tableNames\":[\"infra_demo_student\"]}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"data\":\\[(\\d+)].*", "$1");

        String detail = mockMvc.perform(asAdmin(get("/admin-api/infra/codegen/detail").param("tableId", tableId), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.table.tableName").value("infra_demo_student"))
                .andExpect(jsonPath("$.data.columns", hasSize(11)))
                .andReturn().getResponse().getContentAsString();

        // 业务名会成为包名与压缩包内的路径，不合法时在入参校验阶段拒绝
        JsonNode update = new ObjectMapper().readTree(detail).get("data");
        ((ObjectNode) update.get("table")).put("businessName", "../x");
        mockMvc.perform(asAdmin(put("/admin-api/infra/codegen/update"), ADMIN_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(update.toString()))
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(asAdmin(get("/admin-api/infra/codegen/preview").param("tableId", tableId), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(11)))
                .andExpect(jsonPath("$.data[*].filePath", everyItem(not(emptyOrNullString()))))
                .andExpect(jsonPath("$.data[*].code", everyItem(not(emptyOrNullString()))));

        MockHttpServletResponse response = mockMvc.perform(
                        asAdmin(get("/admin-api/infra/codegen/download").param("tableId", tableId), ADMIN_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andReturn().getResponse();
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()),
                StandardCharsets.UTF_8)) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.add(entry.getName());
                assertThat(zip.readAllBytes()).as(entry.getName()).isNotEmpty();
            }
        }
        assertThat(entries).hasSize(11).contains(
                "ccloud-module-infra/ccloud-module-infra-biz/src/main/java/com/ccloud/module/infra/dal/dataobject/demo/DemoStudentDO.java",
                "sql/codegen/infra_demo_student.sql");
    }

    @Test
    void 无预览权限返回403() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/infra/codegen/preview").param("tableId", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }
}
