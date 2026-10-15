package com.ccloud.module.pay.controller.app.order;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.service.order.PayOrderService;
import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AppPayOrderControllerTest extends BasePayWebIntegrationTest {

    private static final long MEMBER_ID = 82001L;
    private static final long OTHER_ID = 82002L;

    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;
    @Resource
    private PayOrderService orderService;

    private Long myOrderId;
    private Long externalOrderId;
    private Long adminOrderId;

    @BeforeEach
    void setUp() {
        cleanUp();
        TenantUtils.execute(TENANT, () -> {
            PayAppDO app = new PayAppDO().setAppKey("webtest-cashier").setName("收银台测试").setStatus(0);
            appMapper.insert(app);
            channelMapper.insert(new PayChannelDO().setAppId(app.getId()).setCode("mock").setStatus(0).setFeeRate(0D)
                    .setConfig("{\"notifySecret\":\"0123456789abcdef-webtest\",\"behavior\":\"DROP_NOTIFY\"}"));
            myOrderId = create("WT-C1", MEMBER_ID, UserTypeEnum.MEMBER);
            externalOrderId = create("WT-C2", null, null);
            // 付款人编号相同但不是会员：不能被同编号的会员看到
            adminOrderId = create("WT-C3", MEMBER_ID, UserTypeEnum.ADMIN);
        });
    }

    private Long create(String merchantOrderId, Long userId, UserTypeEnum userType) {
        return orderService.createOrder(new PayOrderCreateReqDTO().setAppKey("webtest-cashier")
                .setMerchantOrderId(merchantOrderId).setSubject("收银台").setPrice(100)
                .setExpireTime(LocalDateTime.now().plusHours(1)).setUserIp("127.0.0.1")
                .setUserId(userId).setUserType(userType == null ? null : userType.getValue()));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE e FROM pay_order_extension e JOIN pay_order o ON e.order_id = o.id "
                + "JOIN pay_app a ON o.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE o FROM pay_order o JOIN pay_app a ON o.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
    }

    private String submitBody(Long id) {
        return "{\"id\":" + id + ",\"channelCode\":\"mock\"}";
    }

    @Test
    void 会员查看并支付本人订单_返回渠道展示信息() throws Exception {
        mockMvc.perform(asMember(get("/app-api/pay/order/get").param("id", String.valueOf(myOrderId)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.price").value(100))
                .andExpect(jsonPath("$.data.subject").value("收银台"))
                .andExpect(jsonPath("$.data.status").value(0))
                // 会员端不暴露外部系统才关心的字段
                .andExpect(jsonPath("$.data.merchantOrderId").doesNotExist())
                .andExpect(jsonPath("$.data.userIp").doesNotExist());
        mockMvc.perform(asMember(post("/app-api/pay/order/submit").contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(myOrderId)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.displayMode").value("URL"))
                .andExpect(jsonPath("$.data.displayContent").value(startsWith("/app-api/pay/mock/confirm?no=")));
        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pay_order_extension WHERE order_id = ?", Integer.class, myOrderId);
        assertThat(attempts).isEqualTo(1);
    }

    @Test
    void 不能查看或支付他人与外部系统的订单_不存在的订单同样报不存在() throws Exception {
        // 他人的订单
        mockMvc.perform(asMember(get("/app-api/pay/order/get").param("id", String.valueOf(myOrderId)), OTHER_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
        mockMvc.perform(asMember(post("/app-api/pay/order/submit").contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(myOrderId)), OTHER_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
        // 外部系统的订单没有付款人
        mockMvc.perform(asMember(get("/app-api/pay/order/get").param("id", String.valueOf(externalOrderId)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
        mockMvc.perform(asMember(post("/app-api/pay/order/submit").contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(externalOrderId)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
        // 付款人编号相同但付款人类型不是会员
        mockMvc.perform(asMember(get("/app-api/pay/order/get").param("id", String.valueOf(adminOrderId)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
        mockMvc.perform(asMember(post("/app-api/pay/order/submit").contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(adminOrderId)), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
        // 订单不存在：与上面完全一致，不泄露订单是否存在
        mockMvc.perform(asMember(get("/app-api/pay/order/get").param("id", "-1"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(1_007_002_000));
        // 被拒绝的提交没有产生支付尝试
        Integer attempts = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_order_extension e "
                + "JOIN pay_order o ON e.order_id = o.id JOIN pay_app a ON o.app_id = a.id "
                + "WHERE a.app_key = 'webtest-cashier'", Integer.class);
        assertThat(attempts).isZero();
    }

    @Test
    void 提交参数不合法返回400() throws Exception {
        mockMvc.perform(asMember(post("/app-api/pay/order/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelCode\":\"mock\"}"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(asMember(post("/app-api/pay/order/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + myOrderId + "}"), MEMBER_ID))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void 管理员身份与匿名访问会员接口返回401() throws Exception {
        mockMvc.perform(asAdmin(get("/app-api/pay/order/get").param("id", String.valueOf(myOrderId)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(asAdmin(post("/app-api/pay/order/submit").contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(myOrderId)), ADMIN_ID))
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/app-api/pay/order/get").param("id", String.valueOf(myOrderId)).header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(401));
    }
}
