package com.ccloud.module.pay.controller.app.mock;

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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayMockConfirmControllerTest extends BasePayWebIntegrationTest {

    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;
    @Resource
    private PayOrderService orderService;

    private String no;

    @BeforeEach
    void setUp() {
        cleanup();
        TenantUtils.execute(TENANT, () -> {
            PayAppDO app = new PayAppDO().setAppKey("webtest-confirm").setName("确认页测试").setStatus(0);
            appMapper.insert(app);
            PayChannelDO channel = new PayChannelDO().setAppId(app.getId()).setCode("mock").setStatus(0).setFeeRate(0D)
                    .setConfig("{\"notifySecret\":\"0123456789abcdef-webtest\",\"behavior\":\"SUCCESS\",\"delaySeconds\":1}");
            channelMapper.insert(channel);
            Long orderId = orderService.createOrder(new PayOrderCreateReqDTO().setAppKey("webtest-confirm")
                    .setMerchantOrderId("WT-N1").setSubject("<b>回调</b>").setPrice(100)
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

    @Test
    void 确认页展示商品与金额_内容做了转义() throws Exception {
        mockMvc.perform(get("/app-api/pay/mock/confirm").param("no", no))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("1.00")))
                .andExpect(content().string(containsString("method=\"post\"")))
                .andExpect(content().string(containsString("&lt;b&gt;")))
                .andExpect(content().string(not(containsString("<b>"))));
    }

    @Test
    void 确认后回调丢失_补查任务补回支付结果() throws Exception {
        mockMvc.perform(post("/app-api/pay/mock/confirm").param("no", no)).andExpect(status().isOk());
        jdbcTemplate.update("UPDATE pay_order_extension SET create_time = ? WHERE no = ?", LocalDateTime.now().minusMinutes(5), no);
        orderService.syncOrder(100);
        Integer status = jdbcTemplate.queryForObject("SELECT status FROM pay_order WHERE merchant_order_id = 'WT-N1'", Integer.class);
        assertThat(status).isEqualTo(PayOrderStatusEnum.SUCCESS.getStatus());
    }

    @Test
    void 未知单号返回404() throws Exception {
        mockMvc.perform(get("/app-api/pay/mock/confirm").param("no", "P-none")).andExpect(status().isNotFound());
        mockMvc.perform(post("/app-api/pay/mock/confirm").param("no", "P-none")).andExpect(status().isNotFound());
    }
}
