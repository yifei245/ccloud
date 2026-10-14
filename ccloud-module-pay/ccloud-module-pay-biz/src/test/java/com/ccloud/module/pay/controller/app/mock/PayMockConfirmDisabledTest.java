package com.ccloud.module.pay.controller.app.mock;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@TestPropertySource(properties = "ccloud.pay.mock-enabled=false")
class PayMockConfirmDisabledTest extends BasePayWebIntegrationTest {

    @Test
    void 未开启模拟支付时确认页不存在() throws Exception {
        mockMvc.perform(get("/app-api/pay/mock/confirm").param("no", "P1"))
                // 控制器未注册：全局异常处理把"无此路径"包成 HTTP 200 + code 404 的统一响应
                .andExpect(jsonPath("$.code").value(404));
    }
}
