package com.ccloud.module.pay.controller.admin.wallet;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PayWalletRechargeControllerTest extends BasePayWebIntegrationTest {

    private static final long MEMBER_ID = 81201L;

    private Long walletId;
    private Long rechargeId;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO pay_wallet (user_id, user_type, balance, total_expense, total_recharge, "
                + "freeze_price, tenant_id) VALUES (?, 1, 0, 0, 0, 0, ?)", MEMBER_ID, TENANT);
        walletId = jdbcTemplate.queryForObject("SELECT id FROM pay_wallet WHERE user_id = ?", Long.class, MEMBER_ID);
        // 未支付的充值：退款应被拒绝
        jdbcTemplate.update("INSERT INTO pay_wallet_recharge (wallet_id, total_price, pay_price, bonus_price, pay_status, "
                + "refund_status, tenant_id) VALUES (?, 11000, 10000, 1000, b'0', 0, ?)", walletId, TENANT);
        rechargeId = jdbcTemplate.queryForObject("SELECT id FROM pay_wallet_recharge WHERE wallet_id = ?", Long.class, walletId);
    }

    @AfterEach
    void cleanUp() {
        String wallets = "(SELECT id FROM (SELECT id FROM pay_wallet WHERE user_id = " + MEMBER_ID + ") t)";
        jdbcTemplate.update("DELETE FROM pay_wallet_recharge WHERE wallet_id IN " + wallets);
        jdbcTemplate.update("DELETE FROM pay_wallet WHERE user_id = ?", MEMBER_ID);
    }

    @Test
    void 分页能按钱包查到充值记录() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/pay/wallet-recharge/page").param("walletId", String.valueOf(walletId)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(rechargeId))
                .andExpect(jsonPath("$.data.list[0].totalPrice").value(11000));
    }

    @Test
    void 分页返回退款单编号以区分退款中与未退款() throws Exception {
        jdbcTemplate.update("INSERT INTO pay_wallet_recharge (wallet_id, total_price, pay_price, bonus_price, pay_status, "
                + "pay_refund_id, refund_status, tenant_id) VALUES (?, 11000, 10000, 1000, b'1', 4242, 0, ?)", walletId, TENANT);
        mockMvc.perform(asAdmin(get("/admin-api/pay/wallet-recharge/page").param("walletId", String.valueOf(walletId)), ADMIN_ID))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].payRefundId").value(4242))
                .andExpect(jsonPath("$.data.list[1].id").value(rechargeId));
    }

    @Test
    void 未支付的充值退款返回业务错误_不存在的充值返回业务错误() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/pay/wallet-recharge/refund").param("id", String.valueOf(rechargeId)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_007_005_007));
        mockMvc.perform(asAdmin(put("/admin-api/pay/wallet-recharge/refund").param("id", "-1"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_007_005_003));
    }

    @Test
    void 无权限查询或退款返回403() throws Exception {
        mockMvc.perform(asAdmin(put("/admin-api/pay/wallet-recharge/refund").param("id", String.valueOf(rechargeId)), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/pay/wallet-recharge/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }
}
