package com.ccloud.module.infra.controller.admin;

import com.ccloud.module.infra.controller.admin.file.FileController;
import com.ccloud.module.infra.test.BaseInfraWebIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import static com.ccloud.module.infra.framework.file.core.utils.FileTypeUtilsTest.PNG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FileControllerTest extends BaseInfraWebIntegrationTest {

    private static final String FILE_PREFIX = "/admin-api/infra/file/";

    @TempDir
    Path dir;

    @Autowired
    private FileController fileController;

    /**
     * 测试库只有建表脚本，没有种子数据：没有主配置时临时补一条（数据库存储，名称带 Web测试 前缀，不指定编号），
     * 结束后按名称前缀删除；已有主配置则沿用且不改动，断言对照上传返回的地址，不写死配置编号
     */
    @BeforeEach
    void ensureMaster() {
        Integer masters = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM infra_file_config WHERE master = b'1' AND deleted = 0", Integer.class);
        if (masters == 0) {
            jdbcTemplate.update("INSERT INTO infra_file_config (name, storage, master, config) VALUES "
                    + "('Web测试主配置', 1, b'1', '{\"domain\":\"http://127.0.0.1:28080\"}')");
        }
    }

    @AfterEach
    void cleanUp() {
        // 其他记录仍引用的内容不删
        jdbcTemplate.update("DELETE FROM infra_file_content WHERE path IN (SELECT path FROM infra_file WHERE name LIKE 'webtest%') "
                + "AND path NOT IN (SELECT path FROM infra_file WHERE name IS NULL OR name NOT LIKE 'webtest%')");
        jdbcTemplate.update("DELETE FROM infra_file WHERE name LIKE 'webtest%'");
        jdbcTemplate.update("DELETE FROM infra_file_config WHERE name LIKE 'Web测试%'");
    }

    @Test
    void 上传后免登录读取_图片可内联_带安全头() throws Exception {
        String readUri = upload("webtest.png", "image/png", PNG);

        mockMvc.perform(get(URI.create(readUri)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", startsWith("inline")))
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void 文本文件作为附件输出() throws Exception {
        String readUri = upload("webtest.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get(URI.create(readUri)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", startsWith("attachment")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().string("hello"));
    }

    @Test
    void 会员可上传_未登录返回401() throws Exception {
        mockMvc.perform(multipart("/app-api/infra/file/upload").file(new MockMultipartFile("file", "webtest.png", "image/png", PNG))
                        .with(r -> r).header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(asMember(multipart("/app-api/infra/file/upload")
                        .file(new MockMultipartFile("file", "webtest.png", "image/png", PNG)), 100L))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(containsString("/admin-api/infra/file/")));
    }

    @Test
    void 上传SVG被拒绝() throws Exception {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(asAdmin(multipart(FILE_PREFIX + "upload")
                        .file(new MockMultipartFile("file", "webtest.svg", "image/svg+xml", svg)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_002_007_001));
    }

    @Test
    void 读取不存在或不合法的路径返回404() throws Exception {
        String hash = "0".repeat(64);
        long masterId = Long.parseLong(upload("webtest.png", "image/png", PNG).replaceAll(".*/file/(\\d+)/get/.*", "$1"));
        // 不存在的路径、不存在的配置（业务异常也要是 404）
        mockMvc.perform(get(URI.create(FILE_PREFIX + masterId + "/get/20200101/" + hash + ".png")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(URI.create(FILE_PREFIX + "99999999/get/20200101/" + hash + ".png")))
                .andExpect(status().isNotFound());
    }

    /**
     * 路径穿越、NUL 字节、双斜杠在到达控制器前就被 Spring Security 的 HttpFirewall 以 400 拒绝（Tomcat 也会拒绝 %00 与 %2F），
     * 总之不会是 500，更不会返回文件内容
     */
    @Test
    void 畸形路径在入口被拒绝_不返回内容() throws Exception {
        long masterId = Long.parseLong(upload("webtest.png", "image/png", PNG).replaceAll(".*/file/(\\d+)/get/.*", "$1"));
        for (String path : new String[]{"..%2F..%2Fetc%2Fpasswd", "../../etc/passwd", "a%00b.png", "%00", "/etc/passwd"}) {
            mockMvc.perform(get(URI.create(FILE_PREFIX + masterId + "/get/" + path)))
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().string(""));
        }
    }

    /**
     * 绕过防火墙直接调用控制器，确认控制器自身也把这些路径回答为 404：包括数据库存储、本地磁盘存储（其存储器抛
     * FILE_PATH_INVALID 的路径）与不存在的配置
     */
    @Test
    void 控制器自身对畸形路径回答404() throws Exception {
        long masterId = Long.parseLong(upload("webtest.png", "image/png", PNG).replaceAll(".*/file/(\\d+)/get/.*", "$1"));
        jdbcTemplate.update("INSERT INTO infra_file_config (name, storage, master, config) VALUES (?, 10, b'0', ?)",
                "Web测试本地", "{\"basePath\":\"" + dir.toString().replace("\\", "\\\\")
                        + "\",\"domain\":\"http://127.0.0.1:28080\"}");
        long localId = jdbcTemplate.queryForObject("SELECT id FROM infra_file_config WHERE name = 'Web测试本地'", Long.class);
        String[][] cases = {
                {String.valueOf(masterId), "a%00b.png"}, {String.valueOf(masterId), "%00"},
                {String.valueOf(masterId), "..%2F..%2Fetc%2Fpasswd"}, {String.valueOf(masterId), ""},
                {String.valueOf(localId), "a%00b.png"}, {String.valueOf(localId), "/etc/passwd"},
                {String.valueOf(localId), "x/%00"}, {"99999999", "20200101/" + "0".repeat(64) + ".png"}};
        for (String[] c : cases) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", FILE_PREFIX + c[0] + "/get/" + c[1]);
            request.setRequestURI(FILE_PREFIX + c[0] + "/get/" + c[1]);
            MockHttpServletResponse response = new MockHttpServletResponse();
            fileController.getFileContent(request, response, Long.parseLong(c[0]));
            assertThat(response.getStatus()).as(c[0] + " " + c[1]).isEqualTo(404);
            assertThat(response.getContentAsByteArray()).as(c[0] + " " + c[1]).isEmpty();
        }
    }

    /**
     * Spring Security 默认也会加 nosniff，MockMvc 经过安全过滤器所以看不出控制器自己有没有设置；
     * 直接调用控制器，确保该头不依赖过滤器
     */
    @Test
    void 控制器自身设置nosniff_图片内联_文本附件() throws Exception {
        String png = upload("webtest.png", "image/png", PNG);
        String txt = upload("webtest.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        for (String[] c : new String[][]{{png, "inline", "image/png"}, {txt, "attachment", "text/plain"}}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", c[0]);
            request.setRequestURI(c[0]);
            MockHttpServletResponse response = new MockHttpServletResponse();
            fileController.getFileContent(request, response, Long.parseLong(c[0].replaceAll(".*/file/(\\d+)/get/.*", "$1")));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
            assertThat(response.getHeader("Content-Disposition")).startsWith(c[1]);
            assertThat(response.getContentType()).isEqualTo(c[2]);
        }
    }

    @Test
    void 无权限查看分页返回403_管理员可查到上传记录_并可删除() throws Exception {
        upload("webtest.png", "image/png", PNG);

        mockMvc.perform(asAdmin(get(FILE_PREFIX + "page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        MvcResult page = mockMvc.perform(asAdmin(get(FILE_PREFIX + "page").param("name", "webtest.png"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].name").value("webtest.png"))
                .andExpect(jsonPath("$.data.list[0].type").value("image/png"))
                .andReturn();
        Number id = JsonPath.read(page.getResponse().getContentAsString(), "$.data.list[0].id");

        mockMvc.perform(asAdmin(delete(FILE_PREFIX + "delete").param("id", String.valueOf(id)), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(delete(FILE_PREFIX + "delete").param("id", String.valueOf(id)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get(FILE_PREFIX + "page").param("name", "webtest.png"), ADMIN_ID))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void RPC保存文件() throws Exception {
        mockMvc.perform(post("/rpc-api/infra/file/create").header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"webtest-rpc.png\",\"content\":\"" + Base64.getEncoder().encodeToString(PNG) + "\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(containsString("/admin-api/infra/file/")));
    }

    /**
     * 管理员上传，返回读取接口的路径（不含域名）
     */
    private String upload(String name, String contentType, byte[] content) throws Exception {
        String body = mockMvc.perform(asAdmin(multipart(FILE_PREFIX + "upload")
                        .file(new MockMultipartFile("file", name, contentType, content)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(body, "$.data");
        return url.substring(url.indexOf(FILE_PREFIX));
    }
}
