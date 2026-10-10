package com.ccloud.module.open.api.notify;

import com.ccloud.module.open.enums.notify.OpenNotifyStatusEnum;
import com.ccloud.module.open.test.BaseOpenWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class OpenNotifyApiImplTest extends BaseOpenWebIntegrationTest {

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM open_notify_log WHERE app_id IN (SELECT id FROM open_app WHERE name LIKE 'Web测试%')");
        jdbcTemplate.update("DELETE FROM open_app WHERE name LIKE 'Web测试%'");
    }

    @Test
    void 发送返回回调日志编号_记录属于应用所在租户() throws Exception {
        // 应用不配置回调地址：记录直接记为放弃，不发出请求
        String appId = extract(mockMvc.perform(asAdmin(post("/admin-api/open/app/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Web测试notify\",\"status\":0,\"rateLimit\":100}"))
                .andReturn().getResponse().getContentAsString(), "\"id\":(\\d+)");

        String json = mockMvc.perform(post("/rpc-api/open/notify/send").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":" + appId + ",\"event\":\"webtest.rpc\",\"data\":{\"k\":\"v\"}}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(extract(json, "\"data\":(\\d+)"));

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT app_id, event, status, tenant_id FROM open_notify_log WHERE id = ?", id);
        assertThat(row.get("app_id")).isEqualTo(Long.parseLong(appId));
        assertThat(row.get("event")).isEqualTo("webtest.rpc");
        assertThat(((Number) row.get("status")).intValue()).isEqualTo(OpenNotifyStatusEnum.ABANDONED.getStatus());
        assertThat(row.get("tenant_id")).isEqualTo(TENANT);
    }

    @Test
    void 事件为空返回参数错误() throws Exception {
        mockMvc.perform(post("/rpc-api/open/notify/send").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":1,\"event\":\"\",\"data\":{}}"))
                .andExpect(jsonPath("$.code").value(400));
    }

    private static String extract(String json, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(json);
        assertThat(matcher.find()).as(json).isTrue();
        return matcher.group(1);
    }
}
