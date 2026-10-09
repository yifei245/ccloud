package com.ccloud.framework.apilog;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.apilog.core.enums.OperateTypeEnum;
import com.ccloud.framework.apilog.core.filter.ApiAccessLogFilter;
import com.ccloud.framework.apilog.core.service.ApiAccessLogFrameworkService;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.web.config.WebProperties;
import com.ccloud.framework.web.core.filter.CacheRequestBodyFilter;
import com.ccloud.framework.web.core.handler.GlobalResponseBodyHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ApiAccessLogFilterTest {

    private final ApiAccessLogFrameworkService service = mock(ApiAccessLogFrameworkService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DemoController())
                .setControllerAdvice(new GlobalResponseBodyHandler())
                .addFilters(new CacheRequestBodyFilter(), new ApiAccessLogFilter(new WebProperties(), "ut-app", service))
                .build();
    }

    @Test
    void 请求体与查询参数中的敏感字段被替换_嵌套对象也生效() throws Exception {
        mockMvc.perform(post("/admin-api/ut/login").param("token", "t1").param("page", "1")
                .header("X-Tenant-Id", "7").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\",\"profile\":{\"secretKey\":\"s\",\"nick\":\"n\"}}"));
        com.ccloud.framework.apilog.core.service.ApiAccessLog log = captured();
        assertThat(log.getRequestParams()).contains("\"username\":\"admin\"", "\"nick\":\"n\"", "\"page\":\"1\"")
                .doesNotContain("admin123", "\"s\"", "t1");
        assertThat(log.getTenantId()).isEqualTo(7L);
        assertThat(log.getOperateModule()).isEqualTo("单测模块");
        assertThat(log.getOperateName()).isEqualTo("登录");
    }

    @Test
    void 注解列出的字段同样脱敏_且可记录响应体() throws Exception {
        mockMvc.perform(post("/admin-api/ut/card").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardNo\":\"6222000011112222\",\"name\":\"n\"}"));
        com.ccloud.framework.apilog.core.service.ApiAccessLog log = captured();
        assertThat(log.getRequestParams()).doesNotContain("6222000011112222").contains("\"name\":\"n\"");
        assertThat(log.getResponseBody()).contains("\"cardNo\":\"******\"").contains("\"code\":0");
        assertThat(log.getOperateType()).isEqualTo(OperateTypeEnum.CREATE.getType());
    }

    @Test
    void 注解关闭时不记录_关闭请求参数时不记参数() throws Exception {
        mockMvc.perform(get("/admin-api/ut/silent"));
        verify(service, never()).createApiAccessLogAsync(any());
        mockMvc.perform(post("/admin-api/ut/no-params").contentType(MediaType.APPLICATION_JSON).content("{\"a\":\"b\"}"));
        assertThat(captured().getRequestParams()).isNull();
    }

    @Test
    void 非JSON请求体不原样记录() throws Exception {
        mockMvc.perform(post("/admin-api/ut/login").contentType(MediaType.APPLICATION_JSON).content("password=abc"));
        assertThat(captured().getRequestParams()).doesNotContain("abc").contains("无法解析的请求体");
    }

    @Test
    void 租户编号请求头格式不对时仍记录日志_租户编号为空() throws Exception {
        mockMvc.perform(post("/admin-api/ut/login").header("X-Tenant-Id", "x").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"));
        com.ccloud.framework.apilog.core.service.ApiAccessLog log = captured();
        assertThat(log.getTenantId()).isNull();
        assertThat(log.getRequestParams()).contains("\"username\":\"admin\"").doesNotContain("admin123");
        assertThat(log.getOperateName()).isEqualTo("登录");
    }

    private com.ccloud.framework.apilog.core.service.ApiAccessLog captured() {
        ArgumentCaptor<com.ccloud.framework.apilog.core.service.ApiAccessLog> captor =
                ArgumentCaptor.forClass(com.ccloud.framework.apilog.core.service.ApiAccessLog.class);
        verify(service, atLeastOnce()).createApiAccessLogAsync(captor.capture());
        return captor.getValue();
    }

    @Tag(name = "单测模块")
    @RestController
    @RequestMapping("/admin-api/ut")
    static class DemoController {

        @PostMapping("/login")
        @Operation(summary = "登录")
        public CommonResult<Boolean> login(@RequestBody(required = false) Map<String, Object> body) {
            return CommonResult.success(true);
        }

        @PostMapping("/card")
        @ApiAccessLog(responseEnable = true, sanitizeKeys = "cardNo", operateType = OperateTypeEnum.CREATE)
        public CommonResult<Map<String, Object>> card(@RequestBody Map<String, Object> body) {
            return CommonResult.success(body);
        }

        @GetMapping("/silent")
        @ApiAccessLog(enable = false)
        public CommonResult<Boolean> silent() {
            return CommonResult.success(true);
        }

        @PostMapping("/no-params")
        @ApiAccessLog(requestEnable = false)
        public CommonResult<Boolean> noParams(@RequestBody Map<String, Object> body) {
            return CommonResult.success(true);
        }
    }
}
