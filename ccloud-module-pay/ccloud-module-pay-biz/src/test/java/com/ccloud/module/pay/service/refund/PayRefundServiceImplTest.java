package com.ccloud.module.pay.service.refund;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.dal.dataobject.refund.PayRefundDO;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.order.PayOrderStatusEnum;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundUnifiedReq;
import com.ccloud.module.pay.mq.message.PayRefundMessage;
import com.ccloud.module.pay.service.app.PayAppServiceImpl;
import com.ccloud.module.pay.service.channel.PayChannelServiceImpl;
import com.ccloud.module.pay.service.common.PayAfterCommitExecutor;
import com.ccloud.module.pay.service.order.PayOrderServiceImpl;
import com.ccloud.module.pay.support.FakePayClientProvider;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import({PayRefundServiceImpl.class, PayOrderServiceImpl.class, PayAppServiceImpl.class, PayChannelServiceImpl.class,
        PayClientFactory.class, FakePayClientProvider.class, PayNoRedisDAO.class, PayProperties.class,
        PayAfterCommitExecutor.class})
class PayRefundServiceImplTest extends PayRefundTestBase {

    @Resource
    private PlatformTransactionManager transactionManager;

    @Test
    void 发起退款_预占额度_调用渠道且回调地址指向本渠道() {
        Long id = refundService.createRefund(refundReq("RF1", 300));
        PayRefundDO refund = refundMapper.selectById(id);
        assertThat(refund.getStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
        assertThat(orderMapper.selectById(paidOrderId).getRefundPrice()).isEqualTo(300);
        PayRefundUnifiedReq req = fakeProvider.client().lastRefundReq;
        assertThat(req.getOutRefundNo()).isEqualTo(refund.getNo());
        assertThat(req.getOutTradeNo()).isEqualTo(orderMapper.selectById(paidOrderId).getNo());
        assertThat(req.getNotifyUrl()).endsWith("/app-api/pay/notify/refund/" + channel.getId());
    }

    @Test
    void 重复发起返回原退款单_订单或金额不同报错() {
        Long id = refundService.createRefund(refundReq("RF2", 300));
        assertThat(refundService.createRefund(refundReq("RF2", 300))).isEqualTo(id);
        assertServiceException(() -> refundService.createRefund(refundReq("RF2", 301)), PAY_REFUND_EXISTS_MISMATCH);
    }

    @Test
    void 已有退款在处理中时不能再发起() {
        refundService.createRefund(refundReq("RF3", 100));
        assertServiceException(() -> refundService.createRefund(refundReq("RF4", 100)), PAY_REFUND_HAS_REFUNDING);
    }

    @Test
    void 退款金额超过可退金额报错_未支付订单不能退款() {
        assertServiceException(() -> refundService.createRefund(refundReq("RF5", 1001)), PAY_REFUND_PRICE_EXCEED);
        orderService.createOrder(new PayOrderCreateReqDTO().setAppKey("ut-refund").setMerchantOrderId("M-UNPAID")
                .setSubject("s").setPrice(100).setExpireTime(LocalDateTime.now().plusHours(1)).setUserIp("1.1.1.1"));
        assertServiceException(() -> refundService.createRefund(refundReq("RF6", 1).setMerchantOrderId("M-UNPAID")),
                PAY_REFUND_ORDER_STATUS_ERROR);
    }

    @Test
    void 退款成功_部分退款订单仍为支付成功_全部退完置为已退款_写退款成功消息() {
        Long first = refundService.createRefund(refundReq("RF7", 400));
        refundService.notifyRefund(channel, PayRefundResult.success(refundMapper.selectById(first).getNo(), "CR1", 400, LocalDateTime.now(), "{}"));
        assertThat(orderMapper.selectById(paidOrderId).getStatus()).isEqualTo(PayOrderStatusEnum.SUCCESS.getStatus());

        Long second = refundService.createRefund(refundReq("RF8", 600));
        refundService.notifyRefund(channel, PayRefundResult.success(refundMapper.selectById(second).getNo(), "CR2", 600, LocalDateTime.now(), "{}"));
        assertThat(orderMapper.selectById(paidOrderId).getStatus()).isEqualTo(PayOrderStatusEnum.REFUND.getStatus());
        verify(outboxMessageService, times(2)).saveAndPublish(eq(PayRefundMessage.TOPIC_SUCCESS), any());
    }

    @Test
    void 退款失败释放预占额度_写退款失败消息() {
        Long id = refundService.createRefund(refundReq("RF9", 300));
        refundService.notifyRefund(channel, PayRefundResult.failure(refundMapper.selectById(id).getNo(), "E", "余额不足", "{}"));
        assertThat(refundMapper.selectById(id).getStatus()).isEqualTo(PayRefundStatusEnum.FAILURE.getStatus());
        assertThat(orderMapper.selectById(paidOrderId).getRefundPrice()).isZero();
        verify(outboxMessageService).saveAndPublish(eq(PayRefundMessage.TOPIC_FAILURE), any());
    }

    @Test
    void 回调退款金额不一致时拒绝() {
        Long id = refundService.createRefund(refundReq("RF10", 300));
        assertServiceException(() -> refundService.notifyRefund(channel,
                PayRefundResult.success(refundMapper.selectById(id).getNo(), "CR", 1, LocalDateTime.now(), "{}")), PAY_REFUND_NOTIFY_PRICE_MISMATCH);
        assertThat(refundMapper.selectById(id).getStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
    }

    @Test
    void 渠道同步返回成功时立即完成() {
        fakeProvider.client().onUnifiedRefund = req -> PayRefundResult.success(req.getOutRefundNo(), "CR", req.getRefundPrice(), LocalDateTime.now(), "{}");
        Long id = refundService.createRefund(refundReq("RF11", 100));
        assertThat(refundMapper.selectById(id).getStatus()).isEqualTo(PayRefundStatusEnum.SUCCESS.getStatus());
    }

    @Test
    void 补查_查不到结果时重发退款_查到成功后完成() {
        Long id = refundService.createRefund(refundReq("RF12", 100));
        jdbcTemplate.update("UPDATE pay_refund SET create_time = ? WHERE id = ?", LocalDateTime.now().minusMinutes(5), id);
        fakeProvider.client().lastRefundReq = null;
        refundService.syncRefund(100);
        assertThat(fakeProvider.client().lastRefundReq).as("查询仍为等待时按同一退款单号重发").isNotNull();

        fakeProvider.client().onGetRefund = no -> PayRefundResult.success(no, "CR", 100, LocalDateTime.now(), "{}");
        refundService.syncRefund(100);
        assertThat(refundMapper.selectById(id).getStatus()).isEqualTo(PayRefundStatusEnum.SUCCESS.getStatus());
    }

    // 本用例的外层事务由测试管理、结束时回滚，对 PayAfterCommitExecutor 的回归并不敏感；真正守住"提交后才调渠道"的是下面两个让外层事务真正提交的用例
    // （外层事务提交后才调用渠道_同步返回的结果在提交后落库、外层事务提交后调用渠道时不处于任何事务中_…）
    @Test
    @Transactional
    void 在外层事务中发起时_渠道调用推迟到外层提交之后() {
        // 本方法处于事务中（结束时回滚）：发起后渠道尚未被调用
        fakeProvider.client().lastRefundReq = null;
        refundService.createRefund(refundReq("RF13", 100));
        assertThat(fakeProvider.client().lastRefundReq).isNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 外层事务提交后才调用渠道_同步返回的结果在提交后落库() {
        fakeProvider.client().onUnifiedRefund = req -> PayRefundResult.success(req.getOutRefundNo(), "CR", req.getRefundPrice(), LocalDateTime.now(), "{}");
        AtomicInteger callsBeforeCommit = new AtomicInteger(-1);
        Long id = new TransactionTemplate(transactionManager).execute(status -> {
            Long created = refundService.createRefund(refundReq("RF14", 100));
            callsBeforeCommit.set(fakeProvider.client().lastRefundReq == null ? 0 : 1);
            return created;
        });
        assertThat(callsBeforeCommit.get()).as("外层提交前不调用渠道").isZero();
        assertThat(fakeProvider.client().lastRefundReq).as("外层提交后调用渠道").isNotNull();
        // 提交后触发的回写是独立事务：不能随已完成的外层事务一同丢失
        assertThat(refundMapper.selectById(id).getStatus()).isEqualTo(PayRefundStatusEnum.SUCCESS.getStatus());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 外层事务提交后调用渠道时不处于任何事务中_渠道自带的本地事务才会真正提交() {
        AtomicBoolean inTransaction = new AtomicBoolean(true);
        fakeProvider.client().onUnifiedRefund = req -> {
            inTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            return PayRefundResult.waiting(req.getOutRefundNo());
        };
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> refundService.createRefund(refundReq("RF23", 100)));
        assertThat(fakeProvider.client().lastRefundReq).isNotNull();
        assertThat(inTransaction.get()).as("渠道调用时不应有事务（钱包渠道的本地事务会误加入已完成的外层事务）").isFalse();
    }

    @Test
    void 渠道同步返回的金额与退款单不一致时退款保持等待_发起方仍拿到退款单() {
        fakeProvider.client().onUnifiedRefund = req -> PayRefundResult.success(req.getOutRefundNo(), "CR", 1, LocalDateTime.now(), "{}");
        Long id = refundService.createRefund(refundReq("RF24", 100));
        assertThat(refundMapper.selectById(id).getStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
    }

    @Test
    void 并发重复发起同一商户退款单号_后到者拿到原退款单而不是异常() {
        fakeProvider.client().onUnifiedRefund = req -> PayRefundResult.success(req.getOutRefundNo(), "CR", req.getRefundPrice(), LocalDateTime.now(), "{}");
        Long first = refundService.createRefund(refundReq("RF25", 300));
        // 模拟后到者在先到者提交之前查过一次、没查到：它随后预占额度、插入时撞上唯一键
        doReturn(null).doCallRealMethod().when(refundMapper).selectByAppIdAndMerchantRefundId(any(), eq("RF25"));

        assertThat(refundService.createRefund(refundReq("RF25", 300))).isEqualTo(first);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_refund WHERE order_id = ?", Integer.class, paidOrderId)).isEqualTo(1);
        assertThat(orderMapper.selectById(paidOrderId).getRefundPrice()).as("后到者的预占额度随事务回滚").isEqualTo(300);
    }

    @Test
    void 并发重复发起同一商户退款单号但金额不同_后到者报不一致且额度不变() {
        fakeProvider.client().onUnifiedRefund = req -> PayRefundResult.success(req.getOutRefundNo(), "CR", req.getRefundPrice(), LocalDateTime.now(), "{}");
        refundService.createRefund(refundReq("RF26", 300));
        doReturn(null).doCallRealMethod().when(refundMapper).selectByAppIdAndMerchantRefundId(any(), eq("RF26"));

        assertServiceException(() -> refundService.createRefund(refundReq("RF26", 301)), PAY_REFUND_EXISTS_MISMATCH);

        assertThat(orderMapper.selectById(paidOrderId).getRefundPrice()).isEqualTo(300);
    }

    @Test
    void 后到的重复请求撞上额度不足时返回原退款单而不是额度错误() {
        fakeProvider.client().onUnifiedRefund = req -> PayRefundResult.success(req.getOutRefundNo(), "CR", req.getRefundPrice(), LocalDateTime.now(), "{}");
        Long first = refundService.createRefund(refundReq("RF33", 600));
        // 后到者在先到者提交前查重未命中；其预占是对最新已提交行的更新，600 + 600 > 1000 而被拒
        doReturn(null).doCallRealMethod().when(refundMapper).selectByAppIdAndMerchantRefundId(any(), eq("RF33"));

        assertThat(refundService.createRefund(refundReq("RF33", 600))).isEqualTo(first);

        assertThat(orderMapper.selectById(paidOrderId).getRefundPrice()).isEqualTo(600);
    }

    @Test
    void 后到的重复请求撞上先到者处理中时返回原退款单而不是处理中错误() {
        Long first = refundService.createRefund(refundReq("RF34", 300));
        assertThat(refundMapper.selectById(first).getStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
        doReturn(null).doCallRealMethod().when(refundMapper).selectByAppIdAndMerchantRefundId(any(), eq("RF34"));

        assertThat(refundService.createRefund(refundReq("RF34", 300))).isEqualTo(first);
    }

    @Test
    void 后到的重复请求撞上额度不足但金额与原退款单不同时报不一致() {
        fakeProvider.client().onUnifiedRefund = req -> PayRefundResult.success(req.getOutRefundNo(), "CR", req.getRefundPrice(), LocalDateTime.now(), "{}");
        refundService.createRefund(refundReq("RF35", 600));
        doReturn(null).doCallRealMethod().when(refundMapper).selectByAppIdAndMerchantRefundId(any(), eq("RF35"));

        assertServiceException(() -> refundService.createRefund(refundReq("RF35", 700)), PAY_REFUND_EXISTS_MISMATCH);
    }

    @Test
    void 额度不足且没有同号退款单时仍报额度错误() {
        assertServiceException(() -> refundService.createRefund(refundReq("RF36", 1001)), PAY_REFUND_PRICE_EXCEED);
    }

    @Test
    void 订单状态切换的条件在SQL里按当前行判断() {
        Integer success = PayOrderStatusEnum.SUCCESS.getStatus();
        Integer refunded = PayOrderStatusEnum.REFUND.getStatus();
        // 已支付 1000：累计退款 999 时不切换，1000 时切换
        jdbcTemplate.update("UPDATE pay_order SET refund_price = 999 WHERE id = ?", paidOrderId);
        assertThat(orderMapper.updateStatusIfFullyRefunded(paidOrderId, success, refunded)).isZero();
        jdbcTemplate.update("UPDATE pay_order SET refund_price = 1000 WHERE id = ?", paidOrderId);
        assertThat(orderMapper.updateStatusIfFullyRefunded(paidOrderId, success, refunded)).isEqualTo(1);
        assertThat(orderMapper.selectById(paidOrderId).getStatus()).isEqualTo(refunded);
        // 已退款订单：仍等于支付金额时不回退，释放后小于支付金额才回退
        assertThat(orderMapper.updateStatusIfRefundable(paidOrderId, refunded, success)).isZero();
        jdbcTemplate.update("UPDATE pay_order SET refund_price = 999 WHERE id = ?", paidOrderId);
        assertThat(orderMapper.updateStatusIfRefundable(paidOrderId, refunded, success)).isEqualTo(1);
        assertThat(orderMapper.selectById(paidOrderId).getStatus()).isEqualTo(success);
    }

    @Test
    void 一笔退款失败释放额度后_因预占而显示已退款的订单回到支付成功_可再次退款() {
        Long a = refundService.createRefund(refundReq("RF27", 400));
        // 并行存在的另一笔等待中退款 600（预占额度已使订单累计达到 1000）
        PayRefundDO b = new PayRefundDO().setNo("R-UT-B").setAppId(app.getId()).setChannelId(channel.getId())
                .setChannelCode("mock").setOrderId(paidOrderId).setOrderNo(refundMapper.selectById(a).getOrderNo())
                .setMerchantOrderId("M-PAID").setMerchantRefundId("RF28").setStatus(PayRefundStatusEnum.WAITING.getStatus())
                .setPayPrice(1000).setRefundPrice(600).setReason("r").setChannelOrderNo("");
        refundMapper.insert(b);
        jdbcTemplate.update("UPDATE pay_order SET refund_price = 1000 WHERE id = ?", paidOrderId);

        refundService.notifyRefund(channel, PayRefundResult.success(refundMapper.selectById(a).getNo(), "CR", 400, LocalDateTime.now(), "{}"));
        assertThat(orderMapper.selectById(paidOrderId).getStatus()).isEqualTo(PayOrderStatusEnum.REFUND.getStatus());

        refundService.notifyRefund(channel, PayRefundResult.failure("R-UT-B", "E", "余额不足", "{}"));

        PayOrderDO order = orderMapper.selectById(paidOrderId);
        assertThat(order.getStatus()).isEqualTo(PayOrderStatusEnum.SUCCESS.getStatus());
        assertThat(order.getRefundPrice()).isEqualTo(400);
        Long retry = refundService.createRefund(refundReq("RF29", 600));
        assertThat(refundMapper.selectById(retry).getStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
        assertThat(orderMapper.selectById(paidOrderId).getRefundPrice()).isEqualTo(1000);
    }

    @Test
    void 提交后发起渠道退款时出现任何异常都不外传_退款保持等待() {
        // 渠道调用之外的环节出错（这里是读退款单失败）：退款已落库，不能让发起方收到异常
        doThrow(new IllegalStateException("db down")).when(refundMapper).selectById(ArgumentMatchers.<Serializable>any());

        Long id = refundService.createRefund(refundReq("RF32", 100));
        Mockito.reset(refundMapper);

        assertThat(refundMapper.selectById(id).getStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 提交后的渠道调用在发起时的租户下执行() {
        AtomicReference<Long> tenantAtCall = new AtomicReference<>(-1L);
        fakeProvider.client().onUnifiedRefund = req -> {
            tenantAtCall.set(TenantContextHolder.getTenantId());
            return PayRefundResult.waiting(req.getOutRefundNo());
        };
        // 租户上下文只包住发起，外层事务在它之外提交：提交时线程上已没有租户
        TenantContextHolder.clear();
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> TenantUtils.execute(TENANT_ID, () -> refundService.createRefund(refundReq("RF37", 100))));
        assertThat(tenantAtCall.get()).isEqualTo(TENANT_ID);
    }

    @Test
    void 外层事务回滚时不调用渠道_预占额度随之撤销() {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                refundService.createRefund(refundReq("RF15", 100));
                throw new IllegalStateException("外层失败");
            });
        } catch (IllegalStateException expected) {
            // 外层事务回滚
        }
        assertThat(fakeProvider.client().lastRefundReq).isNull();
        assertThat(refundMapper.selectByAppIdAndMerchantRefundId(app.getId(), "RF15")).isNull();
        assertThat(orderMapper.selectById(paidOrderId).getRefundPrice()).isZero();
    }

    @Test
    void 渠道调用异常时退款保持等待_额度保持预占() {
        fakeProvider.client().throwOnCall = new IllegalStateException("timeout");
        Long id = refundService.createRefund(refundReq("RF16", 100));
        assertThat(refundMapper.selectById(id).getStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
        assertThat(orderMapper.selectById(paidOrderId).getRefundPrice()).isEqualTo(100);
    }

    @Test
    void 通知渠道与退款单所属渠道不一致时拒绝且不改任何状态() {
        Long id = refundService.createRefund(refundReq("RF17", 100));
        String no = refundMapper.selectById(id).getNo();
        PayChannelDO other = new PayChannelDO().setAppId(app.getId()).setCode("wx_lite").setStatus(0).setFeeRate(0D).setConfig("{}");
        channelMapper.insert(other);

        assertServiceException(() -> refundService.notifyRefund(other,
                PayRefundResult.success(no, "CR", 100, LocalDateTime.now(), "{}")), PAY_REFUND_NOT_FOUND);
        assertServiceException(() -> refundService.notifyRefund(other,
                PayRefundResult.failure(no, "E", "x", "{}")), PAY_REFUND_NOT_FOUND);

        assertThat(refundMapper.selectById(id).getStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
        assertThat(orderMapper.selectById(paidOrderId).getRefundPrice()).isEqualTo(100);
        verify(outboxMessageService, never()).saveAndPublish(eq(PayRefundMessage.TOPIC_SUCCESS), any());
        verify(outboxMessageService, never()).saveAndPublish(eq(PayRefundMessage.TOPIC_FAILURE), any());
    }

    @Test
    void 通知不存在的退款单报错() {
        assertServiceException(() -> refundService.notifyRefund(channel,
                PayRefundResult.success("R-NOT-EXISTS", "CR", 100, LocalDateTime.now(), "{}")), PAY_REFUND_NOT_FOUND);
    }

    @Test
    void 重复通知只处理一次() {
        Long id = refundService.createRefund(refundReq("RF18", 100));
        PayRefundResult result = PayRefundResult.success(refundMapper.selectById(id).getNo(), "CR", 100, LocalDateTime.now(), "{}");
        refundService.notifyRefund(channel, result);
        refundService.notifyRefund(channel, result);
        verify(outboxMessageService, times(1)).saveAndPublish(eq(PayRefundMessage.TOPIC_SUCCESS), any());
    }

    @Test
    void 补查_每批一条时前一条查询异常不影响后一条() {
        Long first = refundService.createRefund(refundReq("RF19", 100));
        PayRefundDO firstRefund = refundMapper.selectById(first);
        // 第一笔先了结，才能再发起第二笔
        refundService.notifyRefund(channel, PayRefundResult.failure(firstRefund.getNo(), "E", "x", "{}"));
        Long failing = refundService.createRefund(refundReq("RF20", 100));
        String failingNo = refundMapper.selectById(failing).getNo();
        // 第二笔直接插入记录模拟同一订单下并行存在的另一笔等待中退款（编号更大）
        PayRefundDO ok = new PayRefundDO().setNo("R-UT-OK").setAppId(app.getId()).setChannelId(channel.getId())
                .setChannelCode("mock").setOrderId(paidOrderId).setOrderNo(firstRefund.getOrderNo()).setMerchantOrderId("M-PAID")
                .setMerchantRefundId("RF21").setStatus(PayRefundStatusEnum.WAITING.getStatus()).setPayPrice(1000)
                .setRefundPrice(50).setReason("r").setChannelOrderNo("");
        refundMapper.insert(ok);
        jdbcTemplate.update("UPDATE pay_refund SET create_time = ? WHERE id IN (?, ?)", LocalDateTime.now().minusMinutes(5), failing, ok.getId());
        AtomicInteger failingQueries = new AtomicInteger();
        fakeProvider.client().onGetRefund = no -> {
            if (no.equals(failingNo)) {
                // 游标回归时同一条会被反复查询：用 Error 越过 syncRefund 的 catch (Exception) 立即失败，而不是无限循环
                if (failingQueries.incrementAndGet() > 1) {
                    throw new AssertionError("同一条退款被重复查询，游标没有向前推进");
                }
                throw new IllegalStateException("timeout");
            }
            return PayRefundResult.success(no, "CR", 50, LocalDateTime.now(), "{}");
        };

        refundService.syncRefund(1);

        assertThat(refundMapper.selectById(failing).getStatus()).isEqualTo(PayRefundStatusEnum.WAITING.getStatus());
        assertThat(refundMapper.selectById(ok.getId()).getStatus()).isEqualTo(PayRefundStatusEnum.SUCCESS.getStatus());
    }

    @Test
    void 补查条数上限小于1时直接返回_不查询() {
        refundService.createRefund(refundReq("RF38", 100));
        doThrow(new AssertionError("limit 小于 1 不应查询")).when(refundMapper)
                .selectListByStatusAndCreateTimeLt(any(), any(), any(), ArgumentMatchers.anyInt());

        assertThat(refundService.syncRefund(0)).isZero();
        assertThat(refundService.syncRefund(-1)).isZero();
    }

    @Test
    void 补查_所有退款都失败时每条只尝试一次并结束() {
        Long id = refundService.createRefund(refundReq("RF22", 100));
        jdbcTemplate.update("UPDATE pay_refund SET create_time = ? WHERE id = ?", LocalDateTime.now().minusMinutes(5), id);
        AtomicInteger queries = new AtomicInteger();
        fakeProvider.client().onGetRefund = no -> {
            if (queries.incrementAndGet() > 1) {
                throw new AssertionError("同一条退款被重复查询，游标没有向前推进");
            }
            throw new IllegalStateException("timeout");
        };

        refundService.syncRefund(1);

        assertThat(queries.get()).isEqualTo(1);
    }
}
