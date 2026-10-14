package com.ccloud.module.pay.api.order;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PayOrderApiImplTest extends BasePayWebIntegrationTest {

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO pay_app (app_key, name, status, tenant_id) VALUES ('webtest-mall', 'Web测试商城', 0, ?)", TENANT);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pay_order_extension WHERE order_id IN (SELECT id FROM (SELECT o.id FROM pay_order o "
                + "JOIN pay_app a ON a.id = o.app_id WHERE a.app_key LIKE 'webtest-%') t)");
        jdbcTemplate.update("DELETE FROM pay_order WHERE app_id IN (SELECT id FROM (SELECT id FROM pay_app WHERE app_key LIKE 'webtest-%') t)");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
    }

    private static String createBody(int price) {
        return "{\"appKey\":\"webtest-mall\",\"merchantOrderId\":\"WT-API-1\",\"subject\":\"测试商品\",\"price\":" + price
                + ",\"expireTime\":\"2099-01-01T00:00:00\",\"userIp\":\"127.0.0.1\"}";
    }

    private String create(int price) throws Exception {
        return mockMvc.perform(post("/rpc-api/pay/order/create").header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(price)))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void 创建后能查到待支付订单() throws Exception {
        mockMvc.perform(post("/rpc-api/pay/order/create").header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(100)))
                .andExpect(jsonPath("$.code").value(0));
        Long id = jdbcTemplate.queryForObject("SELECT id FROM pay_order WHERE merchant_order_id = 'WT-API-1'", Long.class);

        mockMvc.perform(get("/rpc-api/pay/order/get").header("X-Tenant-Id", TENANT).param("id", String.valueOf(id)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.price").value(100))
                .andExpect(jsonPath("$.data.merchantOrderId").value("WT-API-1"));
    }

    @Test
    void 订单不存在返回空() throws Exception {
        mockMvc.perform(get("/rpc-api/pay/order/get").header("X-Tenant-Id", TENANT).param("id", "-1"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 同一商户单号金额不同返回错误码() throws Exception {
        assertThat(create(100)).contains("\"code\":0");
        mockMvc.perform(post("/rpc-api/pay/order/create").header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(101)))
                .andExpect(jsonPath("$.code").value(1_007_002_003));
        assertThat(jdbcTemplate.queryForList("SELECT id FROM pay_order WHERE merchant_order_id = 'WT-API-1'", Long.class)).hasSize(1);
    }

    @Test
    void 参数不合法返回400() throws Exception {
        mockMvc.perform(post("/rpc-api/pay/order/create").header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(0)))
                .andExpect(jsonPath("$.code").value(400));
    }
}
