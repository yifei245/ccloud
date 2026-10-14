package com.ccloud.module.pay.controller.app.wallet;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AppPayWalletControllerTest extends BasePayWebIntegrationTest {

    private static final long MEMBER_ID = 70102L;
    private static final long OTHER_ID = 70103L;

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pay_wallet_transaction WHERE wallet_id IN "
                + "(SELECT id FROM (SELECT id FROM pay_wallet WHERE user_id IN (?, ?)) t)", MEMBER_ID, OTHER_ID);
        jdbcTemplate.update("DELETE FROM pay_wallet WHERE user_id IN (?, ?)", MEMBER_ID, OTHER_ID);
    }

    @Test
    void 会员首次查询钱包自动开通_余额为0() throws Exception {
        mockMvc.perform(asMember(get("/app-api/pay/wallet/get"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.totalExpense").value(0))
                .andExpect(jsonPath("$.data.totalRecharge").value(0));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_wallet WHERE user_id = ? AND user_type = 1",
                Integer.class, MEMBER_ID)).isEqualTo(1);
    }

    @Test
    void 流水只看到本人钱包的() throws Exception {
        for (long id : new long[]{MEMBER_ID, OTHER_ID}) {
            jdbcTemplate.update("INSERT INTO pay_wallet (user_id, user_type, balance, total_expense, total_recharge, "
                    + "freeze_price, tenant_id) VALUES (?, 1, 10, 0, 0, 0, ?)", id, TENANT);
            Long walletId = jdbcTemplate.queryForObject("SELECT id FROM pay_wallet WHERE user_id = ?", Long.class, id);
            jdbcTemplate.update("INSERT INTO pay_wallet_transaction (no, wallet_id, biz_type, biz_id, title, price, balance, "
                    + "tenant_id) VALUES (?, ?, 6, ?, '后台调整：x', 10, 10, ?)", "WT-" + id, walletId, "WT-biz-" + id, TENANT);
        }
        mockMvc.perform(asMember(get("/app-api/pay/wallet-transaction/page"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].price").value(10));
    }

    @Test
    void 管理员身份访问会员接口返回401() throws Exception {
        mockMvc.perform(asAdmin(get("/app-api/pay/wallet/get"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(401));
    }
}
