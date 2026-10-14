package com.ccloud.module.pay.service.order;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.module.open.api.app.OpenAppApi;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderExtensionDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderExtensionMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderMapper;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.order.PayOrderStatusEnum;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderUnifiedReq;
import com.ccloud.module.pay.mq.message.PayOrderMessage;
import com.ccloud.module.pay.service.app.PayAppServiceImpl;
import com.ccloud.module.pay.service.channel.PayChannelServiceImpl;
import com.ccloud.module.pay.support.FakePayClientProvider;
import com.ccloud.module.pay.test.BasePayDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import({PayOrderServiceImpl.class, PayAppServiceImpl.class, PayChannelServiceImpl.class, PayClientFactory.class,
        FakePayClientProvider.class, PayNoRedisDAO.class, PayProperties.class})
class PayOrderServiceImplTest extends BasePayDbAndRedisUnitTest {

    @Resource
    private PayOrderServiceImpl orderService;
    @Resource
    private PayAppServiceImpl appService;
    @Resource
    private PayOrderMapper orderMapper;
    @Resource
    private PayOrderExtensionMapper extensionMapper;
    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;
    @Resource
    private FakePayClientProvider fakeProvider;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private OutboxMessageService outboxMessageService;
    @MockitoBean
    private OpenAppApi openAppApi;

    private PayAppDO app;
    private PayChannelDO channel;

    @BeforeEach
    void setUp() {
        fakeProvider.client().reset();
        app = new PayAppDO().setAppKey("ut-mall").setName("商城").setStatus(0);
        appMapper.insert(app);
        channel = new PayChannelDO().setAppId(app.getId()).setCode("mock").setStatus(0).setFeeRate(0.6).setConfig("{}");
        channelMapper.insert(channel);
    }

    private PayOrderCreateReqDTO orderReq(String merchantOrderId, int price) {
        return new PayOrderCreateReqDTO().setAppKey("ut-mall").setMerchantOrderId(merchantOrderId).setSubject("测试商品")
                .setPrice(price).setExpireTime(LocalDateTime.now().plusHours(1)).setUserIp("127.0.0.1")
                .setUserId(80001L).setUserType(UserTypeEnum.MEMBER.getValue());
    }

    private PayOrderSubmitReqVO submit(String code) {
        return new PayOrderSubmitReqVO().setChannelCode(code);
    }

    /** 提交支付并返回本次支付尝试 */
    private PayOrderExtensionDO submitAndGetExtension(Long orderId) {
        PayOrderSubmitRespVO resp = orderService.submitOrder(orderId, submit("mock"), "127.0.0.1");
        return extensionMapper.selectByNo(resp.getNo());
    }

    private PayOrderResult success(PayOrderExtensionDO ext, int price) {
        return PayOrderResult.success(ext.getNo(), "CH-" + ext.getNo(), price, LocalDateTime.now(), "{\"raw\":1}");
    }

    @Test
    void 创建支付单_重复创建金额相同返回原单_金额不同报错() {
        Long id = orderService.createOrder(orderReq("M1", 100));
        assertThat(orderService.createOrder(orderReq("M1", 100))).isEqualTo(id);
        assertServiceException(() -> orderService.createOrder(orderReq("M1", 101)), PAY_ORDER_EXISTS_PRICE_MISMATCH);
        PayOrderDO order = orderMapper.selectById(id);
        assertThat(order.getStatus()).isEqualTo(PayOrderStatusEnum.WAITING.getStatus());
        assertThat(order.getRefundPrice()).isZero();
        assertThat(order.getUserId()).isEqualTo(80001L);
    }

    @Test
    void 提交支付返回渠道展示信息_写入等待中的支付尝试_回调地址指向本渠道() {
        Long id = orderService.createOrder(orderReq("M2", 100));
        PayOrderSubmitRespVO resp = orderService.submitOrder(id, submit("mock").setPayMethod("wx_lite"), "10.0.0.1");
        assertThat(resp.getDisplayMode()).isEqualTo("URL");
        assertThat(resp.getStatus()).isEqualTo(PayOrderStatusEnum.WAITING.getStatus());
        PayOrderExtensionDO ext = extensionMapper.selectByNo(resp.getNo());
        assertThat(ext.getStatus()).isEqualTo(PayOrderStatusEnum.WAITING.getStatus());
        assertThat(ext.getPayMethod()).isEqualTo("wx_lite");
        PayOrderUnifiedReq req = fakeProvider.client().lastOrderReq;
        assertThat(req.getNotifyUrl()).endsWith("/app-api/pay/notify/order/" + channel.getId());
        assertThat(req.getPrice()).isEqualTo(100);
        assertThat(req.getPayMethod()).isEqualTo("wx_lite");
    }

    @Test
    void 已过期或非待支付的订单不能提交() {
        Long id = orderService.createOrder(orderReq("M3", 100));
        orderMapper.updateById(new PayOrderDO().setId(id).setExpireTime(LocalDateTime.now().minusMinutes(1)));
        assertServiceException(() -> orderService.submitOrder(id, submit("mock"), "1.1.1.1"), PAY_ORDER_IS_EXPIRED);
        orderMapper.updateById(new PayOrderDO().setId(id).setStatus(PayOrderStatusEnum.CLOSED.getStatus()));
        assertServiceException(() -> orderService.submitOrder(id, submit("mock"), "1.1.1.1"), PAY_ORDER_STATUS_IS_NOT_WAITING);
        assertServiceException(() -> orderService.submitOrder(-1L, submit("mock"), "1.1.1.1"), PAY_ORDER_NOT_FOUND);
    }

    @Test
    void 渠道调用异常时支付尝试保持等待并提示稍后查询() {
        Long id = orderService.createOrder(orderReq("M4", 100));
        fakeProvider.client().throwOnCall = new IllegalStateException("timeout");
        assertServiceException(() -> orderService.submitOrder(id, submit("mock"), "1.1.1.1"), PAY_ORDER_SUBMIT_CHANNEL_ERROR);
        assertThat(extensionMapper.selectListByOrderId(id)).singleElement()
                .extracting(PayOrderExtensionDO::getStatus).isEqualTo(PayOrderStatusEnum.WAITING.getStatus());
    }

    @Test
    void 渠道同步返回失败时支付尝试关闭并提示原因_订单仍可换渠道() {
        Long id = orderService.createOrder(orderReq("M5", 100));
        fakeProvider.client().onUnifiedOrder = req -> PayOrderResult.failure(req.getOutTradeNo(), "E1", "卡被拒绝", "{}");
        assertThatThrownBy(() -> orderService.submitOrder(id, submit("mock"), "1.1.1.1"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("卡被拒绝");
        PayOrderExtensionDO ext = extensionMapper.selectListByOrderId(id).get(0);
        assertThat(ext.getStatus()).isEqualTo(PayOrderStatusEnum.CLOSED.getStatus());
        assertThat(ext.getChannelErrorMsg()).isEqualTo("卡被拒绝");
        assertThat(orderMapper.selectById(id).getStatus()).isEqualTo(PayOrderStatusEnum.WAITING.getStatus());
    }

    @Test
    void 支付成功_订单写入渠道与手续费_写一条支付成功消息() {
        Long id = orderService.createOrder(orderReq("M6", 1000));
        PayOrderExtensionDO ext = submitAndGetExtension(id);
        orderService.notifyOrder(channel, success(ext, 1000));

        PayOrderDO order = orderMapper.selectById(id);
        assertThat(order.getStatus()).isEqualTo(PayOrderStatusEnum.SUCCESS.getStatus());
        assertThat(order.getExtensionId()).isEqualTo(ext.getId());
        assertThat(order.getNo()).isEqualTo(ext.getNo());
        assertThat(order.getChannelCode()).isEqualTo("mock");
        assertThat(order.getChannelFeePrice()).isEqualTo(6);
        assertThat(extensionMapper.selectById(ext.getId()).getChannelNotifyData()).isEqualTo("{\"raw\":1}");

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outboxMessageService).saveAndPublish(eq(PayOrderMessage.TOPIC_PAID), payload.capture());
        PayOrderMessage message = (PayOrderMessage) payload.getValue();
        assertThat(message.getAppKey()).isEqualTo("ut-mall");
        assertThat(message.getMerchantOrderId()).isEqualTo("M6");
        assertThat(message.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(message.getPrice()).isEqualTo(1000);
    }

    @Test
    void 重复通知只处理一次() {
        Long id = orderService.createOrder(orderReq("M7", 100));
        PayOrderExtensionDO ext = submitAndGetExtension(id);
        orderService.notifyOrder(channel, success(ext, 100));
        orderService.notifyOrder(channel, success(ext, 100));
        verify(outboxMessageService, times(1)).saveAndPublish(eq(PayOrderMessage.TOPIC_PAID), any());
    }

    @Test
    void 回调金额与订单不一致时拒绝且不改状态() {
        Long id = orderService.createOrder(orderReq("M8", 100));
        PayOrderExtensionDO ext = submitAndGetExtension(id);
        assertServiceException(() -> orderService.notifyOrder(channel, success(ext, 1)), PAY_ORDER_NOTIFY_PRICE_MISMATCH);
        assertThat(extensionMapper.selectById(ext.getId()).getStatus()).isEqualTo(PayOrderStatusEnum.WAITING.getStatus());
        assertThat(orderMapper.selectById(id).getStatus()).isEqualTo(PayOrderStatusEnum.WAITING.getStatus());
        verify(outboxMessageService, never()).saveAndPublish(any(), any());
    }

    @Test
    void 订单已关闭后支付成功记为异常支付() {
        Long id = orderService.createOrder(orderReq("M9", 100));
        PayOrderExtensionDO ext = submitAndGetExtension(id);
        orderMapper.updateById(new PayOrderDO().setId(id).setStatus(PayOrderStatusEnum.CLOSED.getStatus()));
        extensionMapper.updateById(new PayOrderExtensionDO().setId(ext.getId()).setStatus(PayOrderStatusEnum.CLOSED.getStatus()));

        orderService.notifyOrder(channel, success(ext, 100));

        assertThat(extensionMapper.selectById(ext.getId()).getStatus()).isEqualTo(PayOrderStatusEnum.SUCCESS.getStatus());
        assertThat(orderMapper.selectById(id).getStatus()).isEqualTo(PayOrderStatusEnum.CLOSED.getStatus());
        assertThat(orderService.getAbnormalExtensionPage(new PageParam()).getList())
                .extracting(PayOrderExtensionDO::getId).contains(ext.getId());
        verify(outboxMessageService, never()).saveAndPublish(any(), any());
    }

    @Test
    void 同一订单第二笔支付成功记为异常支付() {
        Long id = orderService.createOrder(orderReq("M10", 100));
        PayOrderExtensionDO first = submitAndGetExtension(id);
        PayOrderExtensionDO second = submitAndGetExtension(id);
        orderService.notifyOrder(channel, success(first, 100));
        orderService.notifyOrder(channel, success(second, 100));
        assertThat(orderMapper.selectById(id).getExtensionId()).isEqualTo(first.getId());
        assertThat(orderService.getAbnormalExtensionPage(new PageParam()).getList())
                .extracting(PayOrderExtensionDO::getId).containsExactly(second.getId());
    }

    @Test
    void 钱包支付仅限会员本人订单_充值订单不能用钱包() {
        channelMapper.insert(new PayChannelDO().setAppId(app.getId()).setCode("wallet").setStatus(0).setFeeRate(0D).setConfig("{}"));
        Long external = orderService.createOrder(orderReq("M11", 100).setUserId(null).setUserType(null));
        assertServiceException(() -> orderService.submitOrder(external, submit("wallet"), "1.1.1.1"), PAY_ORDER_WALLET_REQUIRE_MEMBER);

        PayAppDO recharge = new PayAppDO().setAppKey("wallet").setName("钱包充值").setStatus(0);
        appMapper.insert(recharge);
        channelMapper.insert(new PayChannelDO().setAppId(recharge.getId()).setCode("wallet").setStatus(0).setFeeRate(0D).setConfig("{}"));
        Long rechargeOrder = orderService.createOrder(recharge, orderReq("WR1", 100).setAppKey("wallet"));
        assertServiceException(() -> orderService.submitOrder(rechargeOrder, submit("wallet"), "1.1.1.1"), PAY_ORDER_RECHARGE_NOT_ALLOW_WALLET);
    }

    @Test
    void 补查_等待中的支付尝试查到成功后更新订单() {
        Long id = orderService.createOrder(orderReq("M12", 100));
        PayOrderExtensionDO ext = submitAndGetExtension(id);
        jdbcTemplate.update("UPDATE pay_order_extension SET create_time = ? WHERE id = ?", LocalDateTime.now().minusMinutes(5), ext.getId());
        fakeProvider.client().onGetOrder = no -> PayOrderResult.success(no, "CH", 100, LocalDateTime.now(), "{}");

        assertThat(orderService.syncOrder(100)).isGreaterThanOrEqualTo(1);
        assertThat(orderMapper.selectById(id).getStatus()).isEqualTo(PayOrderStatusEnum.SUCCESS.getStatus());
    }

    @Test
    void 超时关单_渠道查到已支付则改为成功不关闭() {
        Long id = orderService.createOrder(orderReq("M13", 100));
        submitAndGetExtension(id);
        orderMapper.updateById(new PayOrderDO().setId(id).setExpireTime(LocalDateTime.now().minusMinutes(1)));
        fakeProvider.client().onGetOrder = no -> PayOrderResult.success(no, "CH", 100, LocalDateTime.now(), "{}");

        orderService.expireOrder(100);
        assertThat(orderMapper.selectById(id).getStatus()).isEqualTo(PayOrderStatusEnum.SUCCESS.getStatus());
    }

    @Test
    void 超时关单_渠道未支付则关闭订单与支付尝试() {
        Long id = orderService.createOrder(orderReq("M14", 100));
        PayOrderExtensionDO ext = submitAndGetExtension(id);
        orderMapper.updateById(new PayOrderDO().setId(id).setExpireTime(LocalDateTime.now().minusMinutes(1)));

        assertThat(orderService.expireOrder(100)).isGreaterThanOrEqualTo(1);
        assertThat(orderMapper.selectById(id).getStatus()).isEqualTo(PayOrderStatusEnum.CLOSED.getStatus());
        assertThat(extensionMapper.selectById(ext.getId()).getStatus()).isEqualTo(PayOrderStatusEnum.CLOSED.getStatus());
    }

    @Test
    void 超时关单_渠道查询异常时本次不关闭() {
        Long id = orderService.createOrder(orderReq("M15", 100));
        submitAndGetExtension(id);
        orderMapper.updateById(new PayOrderDO().setId(id).setExpireTime(LocalDateTime.now().minusMinutes(1)));
        fakeProvider.client().throwOnCall = new IllegalStateException("timeout");

        orderService.expireOrder(100);
        assertThat(orderMapper.selectById(id).getStatus()).isEqualTo(PayOrderStatusEnum.WAITING.getStatus());
    }

    @Test
    void 有支付单的应用不能删除() {
        orderService.createOrder(orderReq("M16", 100));
        assertServiceException(() -> appService.deleteApp(app.getId()), PAY_APP_EXIST_ORDER);
    }
}
