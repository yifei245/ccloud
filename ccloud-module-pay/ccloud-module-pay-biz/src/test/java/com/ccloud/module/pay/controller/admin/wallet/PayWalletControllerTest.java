package com.ccloud.module.pay.controller.admin.wallet;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PayWalletControllerTest extends BasePayWebIntegrationTest {

    private static final long MEMBER_ID = 70101L;

    private Long walletId;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO pay_wallet (user_id, user_type, balance, total_expense, total_recharge, "
                + "freeze_price, tenant_id) VALUES (?, 1, 100, 0, 0, 0, ?)", MEMBER_ID, TENANT);
        walletId = jdbcTemplate.queryForObject("SELECT id FROM pay_wallet WHERE user_id = ?", Long.class, MEMBER_ID);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pay_wallet_transaction WHERE wallet_id IN "
                + "(SELECT id FROM (SELECT id FROM pay_wallet WHERE user_id = ?) t)", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM pay_wallet WHERE user_id = ?", MEMBER_ID);
    }

    private String adjustBody(int price) {
        return "{\"id\":" + walletId + ",\"price\":" + price + ",\"reason\":\"补偿\"}";
    }

    @Test
    void 调整余额后能查到余额与流水() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/pay/wallet/update-balance"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(adjustBody(50)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/pay/wallet/get").param("id", String.valueOf(walletId)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.balance").value(150));
        mockMvc.perform(asAdmin(get("/admin-api/pay/wallet/page").param("userId", String.valueOf(MEMBER_ID)), ADMIN_ID))
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(asAdmin(get("/admin-api/pay/wallet-transaction/page")
                        .param("walletId", String.valueOf(walletId)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("后台调整：补偿"))
                .andExpect(jsonPath("$.data.list[0].price").value(50))
                .andExpect(jsonPath("$.data.list[0].balance").value(150));
    }

    @Test
    void 调整金额为0返回业务错误_缺少原因返回400() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/pay/wallet/update-balance"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(adjustBody(0)))
                .andExpect(jsonPath("$.code").value(1_007_005_002));
        mockMvc.perform(asAdmin(put("/admin-api/pay/wallet/update-balance"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + walletId + ",\"price\":1,\"reason\":\"\"}"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void 无权限调整返回403() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/pay/wallet/update-balance"), NO_PERM_ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(adjustBody(50)))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/pay/wallet-transaction/page")
                        .param("walletId", String.valueOf(walletId)), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }
}
