package com.ccloud.module.pay.service.refund;

import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.api.app.OpenAppApi;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderExtensionDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderExtensionMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderMapper;
import com.ccloud.module.pay.dal.mysql.refund.PayRefundMapper;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.service.order.PayOrderServiceImpl;
import com.ccloud.module.pay.support.FakePayClientProvider;
import com.ccloud.module.pay.test.BasePayDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 退款测试的共同准备：退款的渠道调用推迟到事务提交之后，且并发测试需要真实提交，所以本类及子类不在测试事务中运行，
 * 数据真实提交，按应用标识 ut-refund 清理。每个测试开始时有一笔已支付 1000 分的订单 M-PAID
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class PayRefundTestBase extends BasePayDbAndRedisUnitTest {

    @Resource
    protected PayRefundServiceImpl refundService;
    @Resource
    protected PayOrderServiceImpl orderService;
    @Resource
    protected PayOrderMapper orderMapper;
    @Resource
    protected PayOrderExtensionMapper extensionMapper;
    @Resource
    protected PayRefundMapper refundMapper;
    @Resource
    protected PayAppMapper appMapper;
    @Resource
    protected PayChannelMapper channelMapper;
    @Resource
    protected FakePayClientProvider fakeProvider;
    @Resource
    protected JdbcTemplate jdbcTemplate;
    @MockitoBean
    protected OutboxMessageService outboxMessageService;
    @MockitoBean
    protected OpenAppApi openAppApi;

    protected PayAppDO app;
    protected PayChannelDO channel;
    protected Long paidOrderId;

    @BeforeEach
    void setUpPaidOrder() {
        cleanUpRefundData();
        fakeProvider.client().reset();
        TenantUtils.execute(TENANT_ID, () -> {
            app = new PayAppDO().setAppKey("ut-refund").setName("退款测试").setStatus(0);
            appMapper.insert(app);
            channel = new PayChannelDO().setAppId(app.getId()).setCode("mock").setStatus(0).setFeeRate(0.6).setConfig("{}");
            channelMapper.insert(channel);
            paidOrderId = orderService.createOrder(new PayOrderCreateReqDTO().setAppKey("ut-refund").setMerchantOrderId("M-PAID")
                    .setSubject("s").setPrice(1000).setExpireTime(LocalDateTime.now().plusHours(1)).setUserIp("1.1.1.1"));
            String no = orderService.submitOrder(paidOrderId, new PayOrderSubmitReqVO().setChannelCode("mock"), "1.1.1.1").getNo();
            PayOrderExtensionDO ext = extensionMapper.selectByNo(no);
            orderService.notifyOrder(channel, PayOrderResult.success(ext.getNo(), "CH-" + no, 1000, LocalDateTime.now(), "{}"));
        });
        fakeProvider.client().reset();
    }

    @AfterEach
    void cleanUpRefundData() {
        jdbcTemplate.update("DELETE r FROM pay_refund r JOIN pay_app a ON r.app_id = a.id WHERE a.app_key = 'ut-refund'");
        jdbcTemplate.update("DELETE e FROM pay_order_extension e JOIN pay_order o ON e.order_id = o.id "
                + "JOIN pay_app a ON o.app_id = a.id WHERE a.app_key = 'ut-refund'");
        jdbcTemplate.update("DELETE o FROM pay_order o JOIN pay_app a ON o.app_id = a.id WHERE a.app_key = 'ut-refund'");
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key = 'ut-refund'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key = 'ut-refund'");
    }

    protected PayRefundCreateReqDTO refundReq(String merchantRefundId, int price) {
        return new PayRefundCreateReqDTO().setAppKey("ut-refund").setMerchantOrderId("M-PAID")
                .setMerchantRefundId(merchantRefundId).setPrice(price).setReason("不想要了").setUserIp("127.0.0.1");
    }
}
