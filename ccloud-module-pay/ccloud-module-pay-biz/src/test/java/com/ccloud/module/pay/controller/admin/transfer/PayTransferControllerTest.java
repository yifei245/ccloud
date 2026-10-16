package com.ccloud.module.pay.controller.admin.transfer;

import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PayTransferControllerTest extends BasePayWebIntegrationTest {

    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;

    private PayAppDO app;

    @BeforeEach
    void setUp() {
        cleanUp();
        TenantUtils.execute(TENANT, () -> {
            app = new PayAppDO().setAppKey("webtest-transfer").setName("Web测试转账").setStatus(0);
            appMapper.insert(app);
            // DROP_NOTIFY：渠道受理后不回调，转账停留在进行中
            channelMapper.insert(new PayChannelDO().setAppId(app.getId()).setCode("mock").setStatus(0).setFeeRate(0D)
                    .setConfig("{\"notifySecret\":\"0123456789abcdef-webtest\",\"behavior\":\"DROP_NOTIFY\",\"delaySeconds\":1}"));
        });
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE t FROM pay_transfer t JOIN pay_app a ON t.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
    }

    private String createBody(String userAccount) {
        return createBody(app.getId(), userAccount);
    }

    private String createBody(Long appId, String userAccount) {
        return "{\"appId\":" + appId + ",\"channelCode\":\"mock\",\"subject\":\"WT-佣金\",\"price\":200,\"userAccount\":\""
                + userAccount + "\",\"userName\":\"张三\"}";
    }

    @Test
    void 发起转账后分页与详情能查到() throws Exception {
        String response = mockMvc.perform(asAdmin(post("/admin-api/pay/transfer/create")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("WT-ACC-1")), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Long id = jdbcTemplate.queryForObject("SELECT id FROM pay_transfer WHERE user_account = 'WT-ACC-1'", Long.class);

        mockMvc.perform(asAdmin(get("/admin-api/pay/transfer/page").param("userAccount", "WT-ACC-1"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(id))
                .andExpect(jsonPath("$.data.list[0].price").value(200))
                .andExpect(jsonPath("$.data.list[0].status").value(5))
                .andExpect(jsonPath("$.data.list[0].userIp").isNotEmpty());
        mockMvc.perform(asAdmin(get("/admin-api/pay/transfer/get").param("id", String.valueOf(id)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.merchantTransferId").value(startsWith("ADMIN-")));
        assertThat(response).contains("\"data\":" + id);
    }

    @Test
    void 参数不合法或应用不存在时报错() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/pay/transfer/create").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":" + app.getId() + ",\"channelCode\":\"mock\",\"subject\":\"s\",\"price\":0,\"userAccount\":\"a\"}"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(asAdmin(post("/admin-api/pay/transfer/create").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(-1L, "WT-ACC-2")), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_007_000_000));
    }

    @Test
    void 详情不存在时返回错误码() throws Exception {
        mockMvc.perform(asAdmin(get("/admin-api/pay/transfer/get").param("id", "-1"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(1_007_004_000));
    }

    @Test
    void 无权限返回403() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/pay/transfer/create").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("WT-ACC-3")), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/pay/transfer/page"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(asAdmin(get("/admin-api/pay/transfer/get").param("id", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pay_transfer WHERE user_account = 'WT-ACC-3'", Integer.class)).isZero();
    }
}
