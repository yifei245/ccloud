package com.ccloud.module.pay.controller.open;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderExtensionDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderExtensionMapper;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.service.order.PayOrderService;
import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// 钱包充值应用的标识改为带前缀的值，测试不碰真实的 wallet 应用
@TestPropertySource(properties = "ccloud.pay.wallet.recharge-app-key=webtest-recharge")
class OpenPayOrderControllerTest extends BasePayWebIntegrationTest {

    private static final long OPEN_APP_ID = 9001L;
    private static final long RECHARGE_OPEN_APP_ID = 9003L;
    private static final String MOCK_CONFIG = "{\"notifySecret\":\"0123456789abcdef-webtest\",\"behavior\":\"DROP_NOTIFY\"}";

    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;
    @Resource
    private PayOrderExtensionMapper extensionMapper;
    @Resource
    private PayOrderService orderService;

    private PayAppDO app;
    private PayChannelDO mockChannel;

    @BeforeEach
    void setUp() {
        cleanUp();
        TenantUtils.execute(TENANT, () -> {
            app = insertApp("webtest-open", OPEN_APP_ID);
            mockChannel = insertChannel(app.getId(), "mock", 0);
        });
    }

    private PayAppDO insertApp(String appKey, Long openAppId) {
        PayAppDO created = new PayAppDO().setAppKey(appKey).setName(appKey).setStatus(0).setOpenAppId(openAppId);
        appMapper.insert(created);
        return created;
    }

    private PayChannelDO insertChannel(Long appId, String code, int status) {
        PayChannelDO channel = new PayChannelDO().setAppId(appId).setCode(code).setStatus(status).setFeeRate(0D)
                .setConfig("mock".equals(code) ? MOCK_CONFIG : "{}");
        channelMapper.insert(channel);
        return channel;
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE e FROM pay_order_extension e JOIN pay_order o ON e.order_id = o.id "
                + "JOIN pay_app a ON o.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE o FROM pay_order o JOIN pay_app a ON o.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static long hourLater() {
        return LocalDateTimeUtil.toEpochMilli(LocalDateTime.now().plusHours(1).withNano(0));
    }

    private String createBody(String merchantOrderId, Object expireTime) {
        return "{\"merchantOrderId\":\"" + merchantOrderId + "\",\"subject\":\"开放下单\",\"price\":100,\"expireTime\":"
                + expireTime + "}";
    }

    private Long createOrder(PayAppDO owner, String merchantOrderId) {
        return TenantUtils.execute(TENANT, () -> orderService.createOrder(owner, new PayOrderCreateReqDTO()
                .setAppKey(owner.getAppKey()).setMerchantOrderId(merchantOrderId).setSubject("订单").setPrice(100)
                .setExpireTime(LocalDateTime.now().plusHours(1)).setUserIp("127.0.0.1")));
    }

    @Test
    void 开放应用创建并查询支付单_重复创建返回同一单() throws Exception {
        long expire = hourLater();
        String body = createBody("WT-O1", expire);
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/create"), body), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.merchantOrderId").value("WT-O1"))
                .andExpect(jsonPath("$.data.price").value(100))
                // 外部系统用商户单号定位订单，不返回内部编号
                .andExpect(jsonPath("$.data.id").doesNotExist());
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/create"), body), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asOpenApp(get("/open-api/pay/order/get").param("merchantOrderId", "WT-O1"), OPEN_APP_ID))
                .andExpect(jsonPath("$.data.merchantOrderId").value("WT-O1"));
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_order WHERE merchant_order_id = 'WT-O1'", Integer.class);
        assertThat(count).isEqualTo(1);
        // 外部订单没有付款会员
        Long userId = jdbcTemplate.queryForObject("SELECT user_id FROM pay_order WHERE merchant_order_id = 'WT-O1'", Long.class);
        assertThat(userId).isNull();
    }

    @Test
    void 同一商户订单号金额不同_创建被拒绝() throws Exception {
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/create"), createBody("WT-O1", hourLater())), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0));
        String other = createBody("WT-O1", hourLater()).replace("\"price\":100", "\"price\":101");
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/create"), other), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_003));
    }

    @Test
    void 时间字段统一为毫秒时间戳_请求只接受数字_响应输出数字() throws Exception {
        long expire = hourLater();
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/create"), createBody("WT-O1", expire)), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.expireTime").value(expire))
                // 尚未支付：键存在，值为空
                .andExpect(jsonPath("$.data.successTime").doesNotExist());
        LocalDateTime stored = jdbcTemplate.queryForObject(
                "SELECT expire_time FROM pay_order WHERE merchant_order_id = 'WT-O1'", LocalDateTime.class);
        assertThat(LocalDateTimeUtil.toEpochMilli(stored)).isEqualTo(expire);

        // 支付成功后 successTime 同样是毫秒时间戳
        Long orderId = jdbcTemplate.queryForObject("SELECT id FROM pay_order WHERE merchant_order_id = 'WT-O1'", Long.class);
        LocalDateTime successTime = LocalDateTime.now().withNano(0);
        TenantUtils.execute(TENANT, () -> {
            String no = orderService.submitOrder(orderId, new PayOrderSubmitReqVO().setChannelCode("mock"), "127.0.0.1").getNo();
            PayOrderExtensionDO ext = extensionMapper.selectByNo(no);
            orderService.notifyOrder(mockChannel, PayOrderResult.success(ext.getNo(), "CH-" + no, 100, successTime, "{}"));
        });
        mockMvc.perform(asOpenApp(get("/open-api/pay/order/get").param("merchantOrderId", "WT-O1"), OPEN_APP_ID))
                .andExpect(jsonPath("$.data.status").value(10))
                .andExpect(jsonPath("$.data.successTime").value(LocalDateTimeUtil.toEpochMilli(successTime)))
                .andExpect(jsonPath("$.data.expireTime").value(expire));
    }

    @Test
    void 过期时间不是毫秒时间戳或已过期_返回400() throws Exception {
        String[] bad = {
                "\"2099-01-01T00:00:00\"",            // ISO 字符串
                "\"2099-01-01 00:00:00\"",
                "\"" + hourLater() + "\"",            // 带引号的数字
                "[2099,1,1,0,0,0]",                   // 数组
                "1.5",
                "null",
                String.valueOf(LocalDateTimeUtil.toEpochMilli(LocalDateTime.now().minusMinutes(1)))
        };
        for (String expireTime : bad) {
            mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/create"), createBody("WT-O1", expireTime)), OPEN_APP_ID))
                    .andExpect(jsonPath("$.code").value(400));
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_order WHERE merchant_order_id = 'WT-O1'", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void 创建参数不合法返回400() throws Exception {
        long expire = hourLater();
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/create"), "{\"subject\":\"x\",\"price\":100,\"expireTime\":" + expire + "}"), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/create"),
                createBody("WT-O1", expire).replace("\"price\":100", "\"price\":0")), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void 提交支付返回渠道展示信息() throws Exception {
        createOrder(app, "WT-O2");
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/submit"),
                        "{\"merchantOrderId\":\"WT-O2\",\"channelCode\":\"mock\"}"), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.displayMode").value("URL"))
                .andExpect(jsonPath("$.data.displayContent").value(startsWith("/app-api/pay/mock/confirm?no=")));
    }

    @Test
    void 提交支付不能选钱包_即使订单不存在也先于业务校验拒绝() throws Exception {
        // 订单与钱包渠道都不存在：命中的只能是开放接口自己的前置校验，而不是后面的"订单不存在"或"渠道不存在"
        for (String code : new String[]{"wallet", "WALLET", "Wallet", " wallet "}) {
            mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/submit"),
                            "{\"merchantOrderId\":\"WT-O2\",\"channelCode\":\"" + code + "\"}"), OPEN_APP_ID))
                    .andExpect(jsonPath("$.code").value(1_007_002_005));
        }
        // 订单与启用的钱包渠道都在：同样被拒绝，订单没有产生支付尝试
        TenantUtils.execute(TENANT, () -> insertChannel(app.getId(), "wallet", 0));
        Long orderId = createOrder(app, "WT-O2");
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/submit"),
                        "{\"merchantOrderId\":\"WT-O2\",\"channelCode\":\"wallet\"}"), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_005));
        Integer attempts = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_order_extension WHERE order_id = ?", Integer.class, orderId);
        assertThat(attempts).isZero();
    }

    @Test
    void 未绑定的开放应用不能使用支付() throws Exception {
        mockMvc.perform(asOpenApp(get("/open-api/pay/order/get").param("merchantOrderId", "WT-O2"), 9999L))
                .andExpect(jsonPath("$.code").value(1_007_000_006));
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/create"), createBody("WT-O9", hourLater())), 9999L))
                .andExpect(jsonPath("$.code").value(1_007_000_006));
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/submit"),
                        "{\"merchantOrderId\":\"WT-O2\",\"channelCode\":\"mock\"}"), 9999L))
                .andExpect(jsonPath("$.code").value(1_007_000_006));
    }

    @Test
    void 只能查本应用的订单() throws Exception {
        PayAppDO other = TenantUtils.execute(TENANT, () -> insertApp("webtest-open2", 9002L));
        createOrder(other, "WT-O3");
        mockMvc.perform(asOpenApp(get("/open-api/pay/order/get").param("merchantOrderId", "WT-O3"), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
        mockMvc.perform(asOpenApp(get("/open-api/pay/order/get").param("merchantOrderId", "WT-O3"), 9002L))
                .andExpect(jsonPath("$.code").value(0));
        // 也不能替别的应用的订单提交支付
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/submit"),
                        "{\"merchantOrderId\":\"WT-O3\",\"channelCode\":\"mock\"}"), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
    }

    @Test
    void 钱包充值应用不能经开放平台使用() throws Exception {
        PayAppDO recharge = TenantUtils.execute(TENANT, () -> {
            PayAppDO created = insertApp("webtest-recharge", RECHARGE_OPEN_APP_ID);
            insertChannel(created.getId(), "mock", 0);
            return created;
        });
        createOrder(recharge, "WT-RC1");
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/create"), createBody("WT-RC2", hourLater())), RECHARGE_OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_000_006));
        mockMvc.perform(asOpenApp(get("/open-api/pay/order/get").param("merchantOrderId", "WT-RC1"), RECHARGE_OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_000_006));
        mockMvc.perform(asOpenApp(json(post("/open-api/pay/order/submit"),
                        "{\"merchantOrderId\":\"WT-RC1\",\"channelCode\":\"mock\"}"), RECHARGE_OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(1_007_000_006));
        Integer created = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_order WHERE merchant_order_id = 'WT-RC2'", Integer.class);
        assertThat(created).isZero();
        Integer attempts = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_order_extension e JOIN pay_order o ON e.order_id = o.id "
                + "WHERE o.merchant_order_id = 'WT-RC1'", Integer.class);
        assertThat(attempts).isZero();
    }

    @Test
    void 管理员或会员身份与匿名访问开放接口返回401() throws Exception {
        mockMvc.perform(asAdmin(get("/open-api/pay/order/get").param("merchantOrderId", "x"), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(asMember(get("/open-api/pay/order/get").param("merchantOrderId", "x"), 1L))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(asMember(json(post("/open-api/pay/order/create"), createBody("WT-O9", hourLater())), 1L))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/open-api/pay/order/get").param("merchantOrderId", "x").header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 开放应用身份访问会员端接口返回401() throws Exception {
        mockMvc.perform(asOpenApp(get("/app-api/pay/order/get").param("id", "1"), OPEN_APP_ID))
                .andExpect(jsonPath("$.code").value(401));
    }
}
