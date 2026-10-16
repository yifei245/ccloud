package com.ccloud.module.pay.controller.app.channel;

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
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// 钱包充值应用的标识改为带前缀的值，测试不碰真实的 wallet 应用
@TestPropertySource(properties = "ccloud.pay.wallet.recharge-app-key=webtest-recharge")
class AppPayChannelControllerTest extends BasePayWebIntegrationTest {

    private static final long MEMBER_ID = 82101L;

    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;

    private Long shopAppId;
    private Long walletOffAppId;
    private Long rechargeAppId;

    @BeforeEach
    void setUp() {
        cleanUp();
        TenantUtils.execute(TENANT, () -> {
            // 商城：模拟与钱包开启，微信小程序关闭
            shopAppId = insertApp("webtest-shop", "mock", 0, "wallet", 0, "wx_lite", 1);
            // 钱包渠道已禁用
            walletOffAppId = insertApp("webtest-walletoff", "mock", 0, "wallet", 1, null, 0);
            // 钱包充值应用：钱包渠道即使开启也不能出现
            rechargeAppId = insertApp("webtest-recharge", "mock", 0, "wallet", 0, null, 0);
        });
    }

    private Long insertApp(String appKey, String code1, int status1, String code2, int status2, String code3, int status3) {
        PayAppDO app = new PayAppDO().setAppKey(appKey).setName(appKey).setStatus(0);
        appMapper.insert(app);
        insertChannel(app.getId(), code1, status1);
        insertChannel(app.getId(), code2, status2);
        if (code3 != null) {
            insertChannel(app.getId(), code3, status3);
        }
        return app.getId();
    }

    private void insertChannel(Long appId, String code, int status) {
        channelMapper.insert(new PayChannelDO().setAppId(appId).setCode(code).setStatus(status).setFeeRate(0D).setConfig("{}"));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
    }

    @Test
    void 只返回开启的渠道_带编码与名称() throws Exception {
        mockMvc.perform(asMember(get("/app-api/pay/channel/list").param("appId", String.valueOf(shopAppId)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].code", containsInAnyOrder("mock", "wallet")))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("模拟支付", "钱包支付")));
    }

    @Test
    void 已禁用的钱包渠道不返回() throws Exception {
        mockMvc.perform(asMember(get("/app-api/pay/channel/list").param("appId", String.valueOf(walletOffAppId)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].code", contains("mock")));
    }

    @Test
    void 钱包充值应用不返回钱包渠道() throws Exception {
        mockMvc.perform(asMember(get("/app-api/pay/channel/list").param("appId", String.valueOf(rechargeAppId)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].code", contains("mock")));
    }

    @Test
    void 匿名访问返回401() throws Exception {
        mockMvc.perform(get("/app-api/pay/channel/list").param("appId", String.valueOf(shopAppId)).header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 缺少应用编号返回400_管理员身份返回401() throws Exception {
        mockMvc.perform(asMember(get("/app-api/pay/channel/list"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(asAdmin(get("/app-api/pay/channel/list").param("appId", String.valueOf(shopAppId)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 编码已不受支持的渠道不返回_避免用户选了无法支付的渠道() throws Exception {
        // 遗留的渠道：库里仍是开启状态，但本环境没有对应实现（如关闭模拟支付后遗留的 mock；这里用未实现的 wx_lite 代表）
        Long legacyAppId = TenantUtils.execute(TENANT, () -> insertApp("webtest-legacy", "wx_lite", 0, "wallet", 0, null, 0));

        mockMvc.perform(asMember(get("/app-api/pay/channel/list").param("appId", String.valueOf(legacyAppId)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].code", contains("wallet")));
    }
}
