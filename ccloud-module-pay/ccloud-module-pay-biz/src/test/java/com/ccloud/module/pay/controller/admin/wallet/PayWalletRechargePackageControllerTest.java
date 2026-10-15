package com.ccloud.module.pay.controller.admin.wallet;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PayWalletRechargePackageControllerTest extends BasePayWebIntegrationTest {

    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pay_wallet_recharge_package WHERE name LIKE 'webtest%'");
    }

    private String body(Long id, String name, int payPrice) {
        return "{" + (id == null ? "" : "\"id\":" + id + ",") + "\"name\":\"" + name + "\",\"payPrice\":" + payPrice
                + ",\"bonusPrice\":100,\"status\":0}";
    }

    @Test
    void 增改查删一套流程_重名报业务错误() throws Exception {
        String created = mockMvc.perform(asAdmin(post("/admin-api/pay/wallet-recharge-package/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, "webtest-pkg", 1000)))
                .andExpect(jsonPath("$.code").value(0)).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(created.replaceAll(".*\"data\":(\\d+).*", "$1"));

        mockMvc.perform(asAdmin(post("/admin-api/pay/wallet-recharge-package/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, "webtest-pkg", 2000)))
                .andExpect(jsonPath("$.code").value(1_007_005_006));
        mockMvc.perform(asAdmin(put("/admin-api/pay/wallet-recharge-package/update"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(id, "webtest-pkg", 3000)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/pay/wallet-recharge-package/get").param("id", String.valueOf(id)), ADMIN_ID))
                .andExpect(jsonPath("$.data.payPrice").value(3000));
        mockMvc.perform(asAdmin(get("/admin-api/pay/wallet-recharge-package/page").param("name", "webtest-pkg"), ADMIN_ID))
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(asAdmin(delete("/admin-api/pay/wallet-recharge-package/delete").param("id", String.valueOf(id)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(delete("/admin-api/pay/wallet-recharge-package/delete").param("id", String.valueOf(id)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_007_005_005));
    }

    @Test
    void 参数不合法返回400_无权限返回403() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/pay/wallet-recharge-package/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, "webtest-bad", 0)))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(asAdmin(post("/admin-api/pay/wallet-recharge-package/create"), NO_PERM_ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, "webtest-x", 100)))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/pay/wallet-recharge-package/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }
}
