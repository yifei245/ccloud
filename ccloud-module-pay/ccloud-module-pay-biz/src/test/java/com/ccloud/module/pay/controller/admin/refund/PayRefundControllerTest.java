package com.ccloud.module.pay.controller.admin.refund;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PayRefundControllerTest extends BasePayWebIntegrationTest {

    private Long refundId;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO pay_app (app_key, name, status, tenant_id) VALUES ('webtest-refund', 'Web测试退款', 0, ?)", TENANT);
        Long appId = jdbcTemplate.queryForObject("SELECT id FROM pay_app WHERE app_key = 'webtest-refund'", Long.class);
        jdbcTemplate.update("INSERT INTO pay_refund (no, app_id, channel_id, channel_code, order_id, order_no, merchant_order_id, "
                + "merchant_refund_id, status, pay_price, refund_price, reason, user_ip, channel_order_no, channel_notify_data, tenant_id) "
                + "VALUES ('WT-RNO-1', ?, 1, 'mock', 1, 'WT-ONO-1', 'WT-ORDER-1', 'WT-REFUND-1', 0, 100, 40, '测试', '127.0.0.1', '', "
                + "'{\"raw\":1}', ?)", appId, TENANT);
        refundId = jdbcTemplate.queryForObject("SELECT id FROM pay_refund WHERE merchant_refund_id = 'WT-REFUND-1'", Long.class);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pay_refund WHERE merchant_refund_id LIKE 'WT-%'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
    }

    @Test
    void 分页能按商户退款单号查到() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/pay/refund/page").param("merchantRefundId", "WT-REFUND-1"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(refundId))
                .andExpect(jsonPath("$.data.list[0].refundPrice").value(40));
    }

    @Test
    void 详情带回调报文_不存在时返回错误码() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/pay/refund/get").param("id", String.valueOf(refundId)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.no").value("WT-RNO-1"))
                .andExpect(jsonPath("$.data.channelNotifyData").value("{\"raw\":1}"));
        mockMvc.perform(asAdmin(get("/admin-api/pay/refund/get").param("id", "-1"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_007_003_000));
    }

    @Test
    void 无权限返回403() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/pay/refund/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/pay/refund/get").param("id", String.valueOf(refundId)), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }
}
