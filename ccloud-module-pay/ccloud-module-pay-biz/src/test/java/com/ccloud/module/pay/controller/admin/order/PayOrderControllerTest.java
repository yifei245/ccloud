package com.ccloud.module.pay.controller.admin.order;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PayOrderControllerTest extends BasePayWebIntegrationTest {

    private Long appId;
    private Long orderId;
    private Long extensionId;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO pay_app (app_key, name, status, tenant_id) VALUES ('webtest-order', 'Web测试订单', 0, ?)", TENANT);
        appId = jdbcTemplate.queryForObject("SELECT id FROM pay_app WHERE app_key = 'webtest-order'", Long.class);
        jdbcTemplate.update("INSERT INTO pay_order (app_id, merchant_order_id, subject, price, status, user_ip, expire_time, "
                + "tenant_id) VALUES (?, 'WT-ORDER-1', '测试商品', 100, 0, '127.0.0.1', '2099-01-01 00:00:00', ?)", appId, TENANT);
        orderId = jdbcTemplate.queryForObject("SELECT id FROM pay_order WHERE merchant_order_id = 'WT-ORDER-1'", Long.class);
        // 订单已成功，但成功的是另一笔支付尝试：这一条是异常支付
        jdbcTemplate.update("INSERT INTO pay_order_extension (no, order_id, channel_id, channel_code, user_ip, status, "
                + "channel_notify_data, tenant_id) VALUES ('WT-NO-1', ?, 1, 'mock', '127.0.0.1', 10, '{\"raw\":1}', ?)", orderId, TENANT);
        extensionId = jdbcTemplate.queryForObject("SELECT id FROM pay_order_extension WHERE no = 'WT-NO-1'", Long.class);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pay_order_extension WHERE no LIKE 'WT-%'");
        jdbcTemplate.update("DELETE FROM pay_order WHERE merchant_order_id LIKE 'WT-%'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
    }

    @Test
    void 分页能按商户单号查到订单() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/pay/order/page").param("merchantOrderId", "WT-ORDER-1"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(orderId))
                .andExpect(jsonPath("$.data.list[0].price").value(100));
    }

    @Test
    void 详情带支付尝试与回调报文() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/pay/order/get").param("id", String.valueOf(orderId)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.merchantOrderId").value("WT-ORDER-1"))
                .andExpect(jsonPath("$.data.extensions[0].no").value("WT-NO-1"))
                .andExpect(jsonPath("$.data.extensions[0].channelNotifyData").value("{\"raw\":1}"));
    }

    @Test
    void 订单不存在时详情返回错误码() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/pay/order/get").param("id", "-1"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
    }

    @Test
    void 异常支付分页包含订单未记录的成功尝试() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/pay/order/abnormal-page"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.id == " + extensionId + ")].no").value("WT-NO-1"));
    }

    @Test
    void 无权限返回403() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/pay/order/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/pay/order/get").param("id", String.valueOf(orderId)), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/pay/order/abnormal-page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }
}
