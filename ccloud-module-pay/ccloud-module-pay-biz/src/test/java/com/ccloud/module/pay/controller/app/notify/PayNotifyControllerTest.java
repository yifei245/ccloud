package com.ccloud.module.pay.controller.app.notify;

import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.service.order.PayOrderService;
import com.ccloud.module.pay.service.refund.PayRefundService;
import com.ccloud.module.pay.service.transfer.PayTransferService;
import com.ccloud.module.pay.api.transfer.dto.PayTransferCreateReqDTO;
import com.ccloud.module.pay.enums.transfer.PayTransferStatusEnum;
import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import com.ccloud.module.pay.enums.order.PayOrderStatusEnum;
import com.ccloud.module.pay.framework.pay.core.client.impl.mock.MockPayNotifySigner;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayNotifyControllerTest extends BasePayWebIntegrationTest {

    private static final String NOTIFY_SECRET = "0123456789abcdef-webtest";

    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;
    @MockitoSpyBean
    private PayOrderService orderService;
    @Resource
    private PayRefundService refundService;
    @Resource
    private PayTransferService transferService;
    @Resource
    private PayNotifyController notifyController;

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
        jdbcTemplate.update("DELETE t FROM pay_transfer t JOIN pay_app a ON t.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE r FROM pay_refund r JOIN pay_app a ON r.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE e FROM pay_order_extension e JOIN pay_order o ON e.order_id = o.id "
                + "JOIN pay_app a ON o.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE o FROM pay_order o JOIN pay_app a ON o.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key LIKE 'webtest-%'");
        jdbcTemplate.update("DELETE FROM infra_message_outbox WHERE payload LIKE '%WT-%'");
    }

    private MockHttpServletRequestBuilder signedNotify(String body, String secret) {
        return signedNotify("order", channel.getId(), body, secret);
    }

    private MockHttpServletRequestBuilder signedNotify(String type, Long channelId, String body, String secret) {
        long ts = System.currentTimeMillis();
        return post("/app-api/pay/notify/" + type + "/" + channelId).contentType(MediaType.APPLICATION_JSON).content(body)
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

    @Test
    void 拒绝时应答体不回显错误信息() throws Exception {
        mockMvc.perform(signedNotify(body(1), NOTIFY_SECRET))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("rejected"))
                .andExpect(content().string(not(containsString("金额"))));
    }

    @Test
    void 渠道无法创建客户端时返回500而不是400() throws Exception {
        // wx_lite 本期没有渠道实现，getPayClient 抛 CHANNEL_NOT_SUPPORTED：服务端问题，不是回调方的错
        PayChannelDO unsupported = TenantUtils.execute(TENANT, () -> {
            PayChannelDO c = new PayChannelDO().setAppId(channel.getAppId()).setCode("wx_lite").setStatus(0)
                    .setFeeRate(0D).setConfig("{}");
            channelMapper.insert(c);
            return c;
        });
        long ts = System.currentTimeMillis();
        mockMvc.perform(post("/app-api/pay/notify/order/" + unsupported.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}").header("X-Mock-Timestamp", ts)
                        .header("X-Mock-Sign", MockPayNotifySigner.sign(NOTIFY_SECRET, ts, "{}")))
                .andExpect(status().isInternalServerError()).andExpect(content().string("error"));
    }

    @Test
    void 处理中出现未预期异常返回500() throws Exception {
        doThrow(new IllegalStateException("boom")).when(orderService).notifyOrder(any(), any());
        mockMvc.perform(signedNotify(body(100), NOTIFY_SECRET))
                .andExpect(status().isInternalServerError()).andExpect(content().string("error"));
    }

    @Test
    void 渠道配置无法解密时返回500而不是404或200() throws Exception {
        // 绕过 TypeHandler 直接写入明文配置，读取时解密失败
        jdbcTemplate.update("INSERT INTO pay_channel (code, status, fee_rate, app_id, config, tenant_id) "
                + "VALUES ('wx_pub', 0, 0, ?, 'plaintext-undecryptable', ?)", channel.getAppId(), TENANT);
        Long id = jdbcTemplate.queryForObject("SELECT id FROM pay_channel WHERE app_id = ? AND code = 'wx_pub'",
                Long.class, channel.getAppId());
        mockMvc.perform(post("/app-api/pay/notify/order/" + id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isInternalServerError()).andExpect(content().string("error"));
    }

    /**
     * 把 WT-N1 经签名回调置为已支付，再经服务发起 WT-RF1 的退款（渠道 DROP_NOTIFY：不会自行回调），返回退款单号
     */
    private String paidOrderWithRefund(int price) throws Exception {
        mockMvc.perform(signedNotify(body(100), NOTIFY_SECRET)).andExpect(status().isOk());
        Long id = TenantUtils.execute(TENANT, () -> refundService.createRefund(new PayRefundCreateReqDTO().setAppKey("webtest-notify")
                .setMerchantOrderId("WT-N1").setMerchantRefundId("WT-RF1").setPrice(price).setReason("测试").setUserIp("127.0.0.1")));
        return jdbcTemplate.queryForObject("SELECT no FROM pay_refund WHERE id = ?", String.class, id);
    }

    private String refundBody(String refundNo, int price) {
        return "{\"type\":\"refund\",\"no\":\"" + refundNo + "\",\"status\":\"SUCCESS\",\"channelNo\":\"MOCK-" + refundNo
                + "\",\"price\":" + price + ",\"successTime\":" + System.currentTimeMillis() + "}";
    }

    private Integer refundStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM pay_refund WHERE merchant_refund_id = 'WT-RF1'", Integer.class);
    }

    @Test
    void 退款回调验签通过后退款成功() throws Exception {
        String refundNo = paidOrderWithRefund(40);
        assertThat(refundStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());

        mockMvc.perform(signedNotify("refund", channel.getId(), refundBody(refundNo, 40), NOTIFY_SECRET))
                .andExpect(status().isOk()).andExpect(content().string("success"));

        assertThat(refundStatus()).isEqualTo(PayRefundStatusEnum.SUCCESS.getStatus());
    }

    @Test
    void 退款回调验签失败或金额不一致返回400且退款不变() throws Exception {
        String refundNo = paidOrderWithRefund(40);

        mockMvc.perform(signedNotify("refund", channel.getId(), refundBody(refundNo, 40), "wrong-secret-000000"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(signedNotify("refund", channel.getId(), refundBody(refundNo, 1), NOTIFY_SECRET))
                .andExpect(status().isBadRequest());

        assertThat(refundStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
    }

    @Test
    void 退款回调来自其他渠道时返回400且退款不变() throws Exception {
        String refundNo = paidOrderWithRefund(40);
        PayChannelDO other = TenantUtils.execute(TENANT, () -> {
            PayAppDO otherApp = new PayAppDO().setAppKey("webtest-notify-other").setName("另一应用").setStatus(0);
            appMapper.insert(otherApp);
            PayChannelDO c = new PayChannelDO().setAppId(otherApp.getId()).setCode("mock").setStatus(0).setFeeRate(0D)
                    .setConfig("{\"notifySecret\":\"" + NOTIFY_SECRET + "\",\"behavior\":\"DROP_NOTIFY\"}");
            channelMapper.insert(c);
            return c;
        });
        // 另一个渠道用自己的密钥验签通过，但退款不属于它
        mockMvc.perform(signedNotify("refund", other.getId(), refundBody(refundNo, 40), NOTIFY_SECRET))
                .andExpect(status().isBadRequest());

        assertThat(refundStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
    }

    /**
     * 经服务发起 WT-TF1 的转账（渠道 DROP_NOTIFY：受理后不会自行回调，为进行中），返回转账单号
     */
    private String processingTransfer() {
        Long id = TenantUtils.execute(TENANT, () -> transferService.createTransfer(new PayTransferCreateReqDTO()
                .setAppKey("webtest-notify").setChannelCode("mock").setMerchantTransferId("WT-TF1").setSubject("回调")
                .setPrice(200).setUserAccount("acc-1").setUserIp("127.0.0.1")));
        return jdbcTemplate.queryForObject("SELECT no FROM pay_transfer WHERE id = ?", String.class, id);
    }

    private String transferBody(String transferNo, String status, int price) {
        return "{\"type\":\"transfer\",\"no\":\"" + transferNo + "\",\"status\":\"" + status + "\",\"channelNo\":\"MOCK-" + transferNo
                + "\",\"price\":" + price + ",\"successTime\":" + System.currentTimeMillis() + "}";
    }

    private Integer transferStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM pay_transfer WHERE merchant_transfer_id = 'WT-TF1'", Integer.class);
    }

    private Integer transferMessages() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infra_message_outbox WHERE payload LIKE '%WT-TF1%'", Integer.class);
    }

    @Test
    void 转账回调验签通过后转账成功并写消息() throws Exception {
        String transferNo = processingTransfer();
        assertThat(transferStatus()).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());

        mockMvc.perform(signedNotify("transfer", channel.getId(), transferBody(transferNo, "SUCCESS", 200), NOTIFY_SECRET))
                .andExpect(status().isOk()).andExpect(content().string("success"));

        assertThat(transferStatus()).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
        assertThat(transferMessages()).isEqualTo(1);
    }

    @Test
    void 转账回调成功金额不一致返回400且转账不变不写消息() throws Exception {
        String transferNo = processingTransfer();

        mockMvc.perform(signedNotify("transfer", channel.getId(), transferBody(transferNo, "SUCCESS", 1), NOTIFY_SECRET))
                .andExpect(status().isBadRequest());

        assertThat(transferStatus()).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
        assertThat(transferMessages()).isZero();
    }

    @Test
    void 入口放行了未支持的类型而处理分支漏加时抛IllegalStateException_由入口应答500() throws Exception {
        // 入口白名单之外的类型到不了这里（404 在前）；直接调用包级的分派方法，守住 default 分支不会静默应答成功
        PayNotifyController target = AopTestUtils.getUltimateTargetObject(notifyController);

        assertThatThrownBy(() -> target.handle("unknown", channel, null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 转账失败回调后转账失败() throws Exception {
        String transferNo = processingTransfer();

        mockMvc.perform(signedNotify("transfer", channel.getId(), transferBody(transferNo, "FAILURE", 200), NOTIFY_SECRET))
                .andExpect(status().isOk());

        assertThat(transferStatus()).isEqualTo(PayTransferStatusEnum.FAILURE.getStatus());
    }

    @Test
    void 转账回调验签失败返回400且转账不变() throws Exception {
        String transferNo = processingTransfer();

        mockMvc.perform(signedNotify("transfer", channel.getId(), transferBody(transferNo, "SUCCESS", 200), "wrong-secret-000000"))
                .andExpect(status().isBadRequest());

        assertThat(transferStatus()).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
        assertThat(transferMessages()).isZero();
    }

    @Test
    void 转账回调来自其他渠道时返回400_转账不变且不写消息() throws Exception {
        String transferNo = processingTransfer();
        PayChannelDO other = TenantUtils.execute(TENANT, () -> {
            PayAppDO otherApp = new PayAppDO().setAppKey("webtest-notify-other").setName("另一应用").setStatus(0);
            appMapper.insert(otherApp);
            PayChannelDO c = new PayChannelDO().setAppId(otherApp.getId()).setCode("mock").setStatus(0).setFeeRate(0D)
                    .setConfig("{\"notifySecret\":\"" + NOTIFY_SECRET + "\",\"behavior\":\"DROP_NOTIFY\"}");
            channelMapper.insert(c);
            return c;
        });
        // 另一个渠道用自己的密钥验签通过，但转账不属于它
        mockMvc.perform(signedNotify("transfer", other.getId(), transferBody(transferNo, "SUCCESS", 200), NOTIFY_SECRET))
                .andExpect(status().isBadRequest());

        assertThat(transferStatus()).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
        assertThat(transferMessages()).isZero();
    }
}
