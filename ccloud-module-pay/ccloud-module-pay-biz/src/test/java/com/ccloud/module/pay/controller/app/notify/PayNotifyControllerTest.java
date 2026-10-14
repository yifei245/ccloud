package com.ccloud.module.pay.controller.app.notify;

import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
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
import com.ccloud.module.pay.enums.order.PayOrderStatusEnum;
import com.ccloud.module.pay.framework.pay.core.client.impl.mock.MockPayNotifySigner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayNotifyControllerTest extends BasePayWebIntegrationTest {

    private static final String NOTIFY_SECRET = "0123456789abcdef-webtest";

    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;
    @Resource
    private PayOrderService orderService;

    private PayChannelDO channel;
    private String no;

    @BeforeEach
    void setUp() {
        cleanup();
        TenantUtils.execute(TENANT, () -> {
            PayAppDO app = new PayAppDO().setAppKey("webtest-notify").setName("回调测试").setStatus(0);
            appMapper.insert(app);
            channel = new PayChannelDO().setAppId(app.getId()).setCode("mock").setStatus(0).setFeeRate(0D)
                    .setConfig("{\"notifySecret\":\"" + NOTIFY_SECRET + "\",\"behavior\":\"DROP_NOTIFY\",\"delaySeconds\":1}");
            channelMapper.insert(channel);
            Long orderId = orderService.createOrder(new PayOrderCreateReqDTO().setAppKey("webtest-notify")
                    .setMerchantOrderId("WT-N1").setSubject("回调").setPrice(100)
                    .setExpireTime(LocalDateTime.now().plusHours(1)).setUserIp("127.0.0.1"));
            no = orderService.submitOrder(orderId, new PayOrderSubmitReqVO().setChannelCode("mock"), "127.0.0.1").getNo();
        });
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE e FROM pay_order_extension e JOIN pay_order o ON e.order_id = o.id "
                + "JOIN pay_app a ON o.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE o FROM pay_order o JOIN pay_app a ON o.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE FROM infra_message_outbox WHERE payload LIKE '%WT-%'");
    }

    private MockHttpServletRequestBuilder signedNotify(String body, String secret) {
        long ts = System.currentTimeMillis();
        return post("/app-api/pay/notify/order/" + channel.getId()).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Mock-Timestamp", ts).header("X-Mock-Sign", MockPayNotifySigner.sign(secret, ts, body));
    }

    private String body(int price) {
        return "{\"type\":\"order\",\"no\":\"" + no + "\",\"status\":\"SUCCESS\",\"channelNo\":\"MOCK-" + no
                + "\",\"price\":" + price + ",\"successTime\":" + System.currentTimeMillis() + "}";
    }

    @Test
    void 验签通过后订单支付成功_不带登录与租户头_应答success() throws Exception {
        mockMvc.perform(signedNotify(body(100), NOTIFY_SECRET))
                .andExpect(status().isOk()).andExpect(content().string("success"));
        Integer status = jdbcTemplate.queryForObject("SELECT status FROM pay_order WHERE merchant_order_id = 'WT-N1'", Integer.class);
        assertThat(status).isEqualTo(PayOrderStatusEnum.SUCCESS.getStatus());
    }

    @Test
    void 验签失败返回400且订单不变() throws Exception {
        mockMvc.perform(signedNotify(body(100), "wrong-secret-000000")).andExpect(status().isBadRequest());
        Integer status = jdbcTemplate.queryForObject("SELECT status FROM pay_order WHERE merchant_order_id = 'WT-N1'", Integer.class);
        assertThat(status).isEqualTo(PayOrderStatusEnum.WAITING.getStatus());
    }

    @Test
    void 金额不一致返回400() throws Exception {
        mockMvc.perform(signedNotify(body(1), NOTIFY_SECRET)).andExpect(status().isBadRequest());
        Integer status = jdbcTemplate.queryForObject("SELECT status FROM pay_order WHERE merchant_order_id = 'WT-N1'", Integer.class);
        assertThat(status).isEqualTo(PayOrderStatusEnum.WAITING.getStatus());
    }

    @Test
    void 渠道不存在或类型未知返回404() throws Exception {
        mockMvc.perform(post("/app-api/pay/notify/order/999999999").content("{}")).andExpect(status().isNotFound());
        mockMvc.perform(post("/app-api/pay/notify/unknown/" + channel.getId()).content("{}")).andExpect(status().isNotFound());
    }
}
