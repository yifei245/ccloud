package com.ccloud.module.pay.controller.open;

import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderExtensionDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderExtensionMapper;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.service.order.PayOrderService;
import com.ccloud.module.pay.service.refund.PayRefundService;
import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// 钱包充值应用的标识改为带前缀的值，测试不碰真实的 wallet 应用
@TestPropertySource(properties = "ccloud.pay.wallet.recharge-app-key=webtest-recharge")
class OpenPayRefundControllerTest extends BasePayWebIntegrationTest {

    private static final long OPEN_APP_ID = 9101L;
    private static final long OTHER_OPEN_APP_ID = 9102L;
    private static final long RECHARGE_OPEN_APP_ID = 9103L;
    private static final String MOCK_CONFIG = "{\"notifySecret\":\"0123456789abcdef-webtest\",\"behavior\":\"DROP_NOTIFY\"}";

    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;
    @Resource
    private PayOrderExtensionMapper extensionMapper;
    @Resource
    private PayOrderService orderService;
    @Resource
    private PayRefundService refundService;

    private PayAppDO app;
    private PayAppDO otherApp;
    private PayAppDO rechargeApp;

    @BeforeEach
    void setUp() {
        cleanUp();
        TenantUtils.execute(TENANT, () -> {
            app = insertAppWithPaidOrder("webtest-refund", OPEN_APP_ID, "WT-R1");
            otherApp = insertAppWithPaidOrder("webtest-refund2", OTHER_OPEN_APP_ID, "WT-R2");
            rechargeApp = insertAppWithPaidOrder("webtest-recharge", RECHARGE_OPEN_APP_ID, "WT-R3");
        });
    }

    /**
     * 建应用、mock 渠道，并让一笔 100 分的订单支付成功
     */
    private PayAppDO insertAppWithPaidOrder(String appKey, long openAppId, String merchantOrderId) {
        PayAppDO created = new PayAppDO().setAppKey(appKey).setName(appKey).setStatus(0).setOpenAppId(openAppId);
        appMapper.insert(created);
        PayChannelDO channel = new PayChannelDO().setAppId(created.getId()).setCode("mock").setStatus(0).setFeeRate(0D)
                .setConfig(MOCK_CONFIG);
        channelMapper.insert(channel);
        Long orderId = orderService.createOrder(created, new PayOrderCreateReqDTO().setAppKey(appKey)
                .setMerchantOrderId(merchantOrderId).setSubject("订单").setPrice(100)
                .setExpireTime(LocalDateTime.now().plusHours(1)).setUserIp("127.0.0.1"));
        String no = orderService.submitOrder(orderId, new PayOrderSubmitReqVO().setChannelCode("mock"), "127.0.0.1").getNo();
        PayOrderExtensionDO ext = extensionMapper.selectByNo(no);
        orderService.notifyOrder(channel, PayOrderResult.success(ext.getNo(), "CH-" + no, 100, LocalDateTime.now(), "{}"));
        return created;
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE r FROM pay_refund r JOIN pay_app a ON r.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE e FROM pay_order_extension e JOIN pay_order o ON e.order_id = o.id "
                + "JOIN pay_app a ON o.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE o FROM pay_order o JOIN pay_app a ON o.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE FROM infra_message_outbox WHERE payload REGEXP '\"WT-R[0-9]+\"'");
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String refundBody(String merchantOrderId, String merchantRefundId, int price) {
        return "{\"merchantOrderId\":\"" + merchantOrderId + "\",\"merchantRefundId\":\"" + merchantRefundId
                + "\",\"price\":" + price + ",\"reason\":\"测试退款\"}";
    }

    private int refundCount(String merchantRefundId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_refund WHERE merchant_refund_id = ?", Integer.class, merchantRefundId);
    }

    @Test
    void 开放应用发起并查询退款_重复发起返回同一退款单() throws Exception {
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/refund/create"), refundBody("WT-R1", "WT-RF1", 30)), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.merchantOrderId").value("WT-R1"))
                .andExpect(jsonPath("$.data.merchantRefundId").value("WT-RF1"))
                .andExpect(jsonPath("$.data.refundPrice").value(30))
                .andExpect(jsonPath("$.data.refundNo").isNotEmpty())
                .andExpect(jsonPath("$.data.id").doesNotExist());
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/refund/create"), refundBody("WT-R1", "WT-RF1", 30)), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0));
        assertThat(refundCount("WT-RF1")).isEqualTo(1);
        mockMvc.perform(asOpenApp(get("/open-api/pay/refund/get").param("merchantRefundId", "WT-RF1"), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.refundPrice").value(30))
                .andExpect(jsonPath("$.data.merchantOrderId").value("WT-R1"));
    }

    @Test
    void 同一商户退款单号金额不同_发起被拒绝() throws Exception {
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/refund/create"), refundBody("WT-R1", "WT-RF1", 30)), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/refund/create"), refundBody("WT-R1", "WT-RF1", 31)), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_003_004));
    }

    @Test
    void 不能对其他应用的订单发起退款() throws Exception {
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/refund/create"), refundBody("WT-R2", "WT-RF9", 30)), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
        assertThat(refundCount("WT-RF9")).isZero();
        // 对照：订单所属的应用可以退
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/refund/create"), refundBody("WT-R2", "WT-RF9", 30)), OTHER_OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0));
        Integer owner = jdbcTemplate.queryForObject("SELECT a.app_key FROM pay_refund r JOIN pay_app a ON r.app_id = a.id "
                + "WHERE r.merchant_refund_id = 'WT-RF9'", (rs, i) -> rs.getString(1).equals("webtest-refund2") ? 1 : 0);
        assertThat(owner).isEqualTo(1);
    }

    @Test
    void 不能查询其他应用的退款单() throws Exception {
        TenantUtils.execute(TENANT, () -> refundService.createRefund(otherApp, new PayRefundCreateReqDTO()
                .setAppKey(otherApp.getAppKey()).setMerchantOrderId("WT-R2").setMerchantRefundId("WT-RF2")
                .setPrice(20).setReason("他人的退款").setUserIp("127.0.0.1")));
        mockMvc.perform(asOpenApp(get("/open-api/pay/refund/get").param("merchantRefundId", "WT-RF2"), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_003_000));
        mockMvc.perform(asOpenApp(get("/open-api/pay/refund/get").param("merchantRefundId", "WT-RF2"), OTHER_OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.refundPrice").value(20));
        mockMvc.perform(asOpenApp(get("/open-api/pay/refund/get").param("merchantRefundId", "WT-NONE"), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_003_000));
    }

    @Test
    void 钱包充值应用不能经开放平台退款() throws Exception {
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/refund/create"), refundBody("WT-R3", "WT-RF3", 30)), RECHARGE_OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_000_006));
        assertThat(refundCount("WT-RF3")).isZero();
        TenantUtils.execute(TENANT, () -> refundService.createRefund(rechargeApp, new PayRefundCreateReqDTO()
                .setAppKey(rechargeApp.getAppKey()).setMerchantOrderId("WT-R3").setMerchantRefundId("WT-RF3")
                .setPrice(20).setReason("内部退款").setUserIp("127.0.0.1")));
        mockMvc.perform(asOpenApp(get("/open-api/pay/refund/get").param("merchantRefundId", "WT-RF3"), RECHARGE_OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_000_006));
    }

    @Test
    void 未绑定的开放应用不能使用退款() throws Exception {
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/refund/create"), refundBody("WT-R1", "WT-RF1", 30)), 9999L))
                .andExpect(jsonPath("$.code").value(1_007_000_006));
        mockMvc.perform(asOpenApp(get("/open-api/pay/refund/get").param("merchantRefundId", "WT-RF1"), 9999L))
                .andExpect(jsonPath("$.code").value(1_007_000_006));
    }

    static Stream<Arguments> refundBodies() {
        return Stream.of(
                Arguments.of("商户退款单号 64 位", body("WT-R1", "r".repeat(64), 30, "原因"), 0),
                Arguments.of("商户退款单号 65 位", body("WT-R1", "r".repeat(65), 30, "原因"), 400),
                Arguments.of("商户退款单号为空", body("WT-R1", "", 30, "原因"), 400),
                Arguments.of("商户订单号 65 位", body("o".repeat(65), "WT-RF1", 30, "原因"), 400),
                // 64 位通过校验，随后因订单不存在报业务错误，而不是 400
                Arguments.of("商户订单号 64 位", body("o".repeat(64), "WT-RF1", 30, "原因"), 1_007_002_000),
                Arguments.of("商户订单号为空", body("", "WT-RF1", 30, "原因"), 400),
                Arguments.of("原因 256 位", body("WT-R1", "WT-RF1", 30, "x".repeat(256)), 0),
                Arguments.of("原因 257 位", body("WT-R1", "WT-RF1", 30, "x".repeat(257)), 400),
                Arguments.of("原因为空", body("WT-R1", "WT-RF1", 30, ""), 400),
                Arguments.of("金额 1", body("WT-R1", "WT-RF1", 1, "原因"), 0),
                Arguments.of("金额 0", body("WT-R1", "WT-RF1", 0, "原因"), 400),
                Arguments.of("金额为负", body("WT-R1", "WT-RF1", -1, "原因"), 400),
                Arguments.of("缺少商户订单号", "{\"merchantRefundId\":\"WT-RF1\",\"price\":30,\"reason\":\"x\"}", 400),
                Arguments.of("缺少金额", "{\"merchantOrderId\":\"WT-R1\",\"merchantRefundId\":\"WT-RF1\",\"reason\":\"x\"}", 400));
    }

    private static String body(String merchantOrderId, String merchantRefundId, int price, String reason) {
        return "{\"merchantOrderId\":\"" + merchantOrderId + "\",\"merchantRefundId\":\"" + merchantRefundId
                + "\",\"price\":" + price + ",\"reason\":\"" + reason + "\"}";
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("refundBodies")
    void 发起退款参数的长度与金额边界(String name, String body, int expectedCode) throws Exception {
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/refund/create"), body), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    @Test
    void 管理员或会员身份访问开放接口返回401() throws Exception {
        mockMvc.perform(asAdmin(json(post("/open-api/pay/refund/create"), refundBody("WT-R1", "WT-RF1", 30)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(asMember(get("/open-api/pay/refund/get").param("merchantRefundId", "WT-RF1"), 1L))
                .andExpect(jsonPath("$.code").value(401));
    }
}
