package com.ccloud.module.infra.controller.admin;

import com.ccloud.module.infra.test.BaseInfraWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class FileConfigControllerTest extends BaseInfraWebIntegrationTest {

    @TempDir
    Path dir;

    /**
     * 测试库只有建表脚本，没有种子数据：补上与生产种子相同的配置 1（数据库存储，主配置）
     */
    @BeforeEach
    void ensureSeedMaster() {
        jdbcTemplate.update("INSERT IGNORE INTO infra_file_config (id, name, storage, master, config) VALUES "
                + "(1, '数据库存储', 1, b'1', '{\"domain\":\"http://127.0.0.1:28080\"}')");
    }

    @AfterEach
    void cleanUp() {
        // 保证配置 1 仍是主配置，之后的测试才能找到主配置
        jdbcTemplate.update("UPDATE infra_file_config SET master = b'0' WHERE id <> 1");
        jdbcTemplate.update("UPDATE infra_file_config SET master = b'1' WHERE id = 1");
        jdbcTemplate.update("DELETE FROM infra_file_config WHERE name LIKE 'Web测试%'");
    }

    @Test
    void 创建本地磁盘配置后读取_配置原样返回() throws Exception {
        String id = mockMvc.perform(asAdmin(post("/admin-api/infra/file-config/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Web测试本地\",\"storage\":10,\"config\":{\"basePath\":\""
                                + dir.toString().replace("\\", "\\\\") + "\",\"domain\":\"http://127.0.0.1:28080\"}}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"data\":(\\d+).*", "$1");
        mockMvc.perform(asAdmin(get("/admin-api/infra/file-config/get").param("id", id), ADMIN_ID))
                .andExpect(jsonPath("$.data.config.basePath").value(dir.toString()))
                .andExpect(jsonPath("$.data.master").value(false));
    }

    @Test
    void 无权限返回403() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/infra/file-config/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void 主配置不能删除() throws Exception {
        mockMvc.perform(asAdmin(delete("/admin-api/infra/file-config/delete").param("id", "1"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_002_006_001));
    }
}
