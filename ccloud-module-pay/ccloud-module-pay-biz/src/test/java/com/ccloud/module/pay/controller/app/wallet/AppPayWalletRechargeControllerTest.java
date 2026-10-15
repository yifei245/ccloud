package com.ccloud.module.pay.controller.app.wallet;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AppPayWalletRechargeControllerTest extends BasePayWebIntegrationTest {

    private static final long MEMBER_ID = 81101L;
    private static final long OTHER_ID = 81102L;

    private Long packageId;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO pay_app (app_key, name, status, tenant_id) VALUES ('wallet', '钱包充值', 0, ?)", TENANT);
        jdbcTemplate.update("INSERT INTO pay_wallet_recharge_package (name, pay_price, bonus_price, status, tenant_id) "
                + "VALUES ('webtest-充100送10', 10000, 1000, 0, ?), ('webtest-closed', 5000, 0, 1, ?)", TENANT, TENANT);
        packageId = jdbcTemplate.queryForObject("SELECT id FROM pay_wallet_recharge_package WHERE name = 'webtest-充100送10'", Long.class);
    }

    @AfterEach
    void cleanUp() {
        String wallets = "(SELECT id FROM (SELECT id FROM pay_wallet WHERE user_id IN (" + MEMBER_ID + ", " + OTHER_ID + ")) t)";
        jdbcTemplate.update("DELETE FROM pay_wallet_recharge WHERE wallet_id IN " + wallets);
        jdbcTemplate.update("DELETE FROM pay_wallet_transaction WHERE wallet_id IN " + wallets);
        jdbcTemplate.update("DELETE FROM pay_wallet WHERE user_id IN (" + MEMBER_ID + ", " + OTHER_ID + ")");
        jdbcTemplate.update("DELETE o FROM pay_order o JOIN pay_app a ON o.app_id = a.id WHERE a.app_key = 'wallet'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key = 'wallet'");
        jdbcTemplate.update("DELETE FROM pay_wallet_recharge_package WHERE name LIKE 'webtest%'");
    }

    private String create(long memberId, String body) throws Exception {
        return mockMvc.perform(asMember(post("/app-api/pay/wallet-recharge/create"), memberId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void 开启的套餐列表不含已关闭的() throws Exception {
        mockMvc.perform(asMember(get("/app-api/pay/wallet-recharge-package/list"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].name", hasItem("webtest-充100送10")))
                .andExpect(jsonPath("$.data[*].name", not(hasItem("webtest-closed"))));
    }

    @Test
    void 会员按套餐充值_返回支付单编号_订单属于该会员() throws Exception {
        mockMvc.perform(asMember(post("/app-api/pay/wallet-recharge/create"), MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"packageId\":" + packageId + "}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.payOrderId").isNumber());
        Long orderId = jdbcTemplate.queryForObject("SELECT pay_order_id FROM pay_wallet_recharge r JOIN pay_wallet w ON r.wallet_id = w.id "
                + "WHERE w.user_id = ?", Long.class, MEMBER_ID);
        assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM pay_order WHERE id = ?", Long.class, orderId)).isEqualTo(MEMBER_ID);
        assertThat(jdbcTemplate.queryForObject("SELECT price FROM pay_order WHERE id = ?", Integer.class, orderId)).isEqualTo(10000);
        assertThat(jdbcTemplate.queryForObject("SELECT merchant_order_id FROM pay_order WHERE id = ?", String.class, orderId)).startsWith("WR");
    }

    @Test
    void 参数不合法返回400_未选套餐也无金额返回业务错误_未开通充值应用返回业务错误() throws Exception {
        mockMvc.perform(asMember(post("/app-api/pay/wallet-recharge/create"), MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"payPrice\":0}"))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(asMember(post("/app-api/pay/wallet-recharge/create"), MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.code").value(1_007_005_008));
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key = 'wallet'");
        mockMvc.perform(asMember(post("/app-api/pay/wallet-recharge/create"), MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"payPrice\":100}"))
                .andExpect(jsonPath("$.code").value(1_007_005_004));
    }

    @Test
    void 充值记录只看到本人的() throws Exception {
        create(MEMBER_ID, "{\"packageId\":" + packageId + "}");
        create(OTHER_ID, "{\"payPrice\":500}");
        mockMvc.perform(asMember(get("/app-api/pay/wallet-recharge/page"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].payPrice").value(10000))
                .andExpect(jsonPath("$.data.list[0].totalPrice").value(11000))
                .andExpect(jsonPath("$.data.list[0].payStatus").value(false));
    }

    @Test
    void 管理员身份访问会员接口返回401() throws Exception {
        mockMvc.perform(asAdmin(post("/app-api/pay/wallet-recharge/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"payPrice\":100}"))
                .andExpect(jsonPath("$.code").value(401));
    }
}
