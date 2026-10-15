package com.ccloud.module.pay.service.transfer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.api.app.OpenAppApi;
import com.ccloud.module.pay.api.transfer.dto.PayTransferCreateReqDTO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.transfer.PayTransferDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.dal.mysql.transfer.PayTransferMapper;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.transfer.PayTransferStatusEnum;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferResult;
import com.ccloud.module.pay.mq.message.PayTransferMessage;
import com.ccloud.module.pay.service.app.PayAppServiceImpl;
import com.ccloud.module.pay.service.channel.PayChannelServiceImpl;
import com.ccloud.module.pay.service.common.PayAfterCommitExecutor;
import com.ccloud.module.pay.support.FakePayClientProvider;
import com.ccloud.module.pay.test.BasePayDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 转账的渠道调用推迟到事务提交之后，且补查需要真实提交，所以本类不在测试事务中运行，数据真实提交，按应用标识 ut-transfer 清理
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({PayTransferServiceImpl.class, PayAppServiceImpl.class, PayChannelServiceImpl.class, PayClientFactory.class,
        FakePayClientProvider.class, PayNoRedisDAO.class, PayProperties.class, PayAfterCommitExecutor.class})
class PayTransferServiceImplTest extends BasePayDbAndRedisUnitTest {

    @Resource
    private PayTransferServiceImpl transferService;
    @MockitoSpyBean
    private PayTransferMapper transferMapper;
    @Resource
    private PayAppMapper appMapper;
    @Resource
    private PayChannelMapper channelMapper;
    @Resource
    private FakePayClientProvider fakeProvider;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private OutboxMessageService outboxMessageService;
    @MockitoBean
    private OpenAppApi openAppApi;

    private PayAppDO app;
    private PayChannelDO channel;

    @BeforeEach
    void setUpApp() {
        cleanUp();
        fakeProvider.client().reset();
        TenantUtils.execute(TENANT_ID, () -> {
            app = new PayAppDO().setAppKey("ut-transfer").setName("转账测试").setStatus(0);
            appMapper.insert(app);
            channel = new PayChannelDO().setAppId(app.getId()).setCode("mock").setStatus(0).setFeeRate(0D).setConfig("{}");
            channelMapper.insert(channel);
            channelMapper.insert(new PayChannelDO().setAppId(app.getId()).setCode("wallet").setStatus(0).setFeeRate(0D).setConfig("{}"));
        });
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE t FROM pay_transfer t JOIN pay_app a ON t.app_id = a.id WHERE a.app_key = 'ut-transfer'");
        jdbcTemplate.update("DELETE c FROM pay_channel c JOIN pay_app a ON c.app_id = a.id WHERE a.app_key = 'ut-transfer'");
        jdbcTemplate.update("DELETE FROM pay_app WHERE app_key = 'ut-transfer'");
    }

    private PayTransferCreateReqDTO req(String merchantTransferId, String code, String account) {
        return new PayTransferCreateReqDTO().setAppKey("ut-transfer").setChannelCode(code)
                .setMerchantTransferId(merchantTransferId).setSubject("佣金").setPrice(200)
                .setUserAccount(account).setUserIp("127.0.0.1");
    }

    private PayTransferResult success(String no) {
        return PayTransferResult.success(no, "CT", 200, LocalDateTime.now(), "{}");
    }

    private Integer statusOf(Long id) {
        return transferMapper.selectById(id).getStatus();
    }

    private void backdate(Long... ids) {
        for (Long id : ids) {
            jdbcTemplate.update("UPDATE pay_transfer SET create_time = ? WHERE id = ?", LocalDateTime.now().minusMinutes(5), id);
        }
    }

    /**
     * 渠道受理未完成（进行中）且已过补查延迟的转账
     */
    private Long processingTransfer(String merchantTransferId) {
        Long id = transferService.createTransfer(req(merchantTransferId, "mock", "acc-1"));
        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
        backdate(id);
        return id;
    }

    @Test
    void 发起后渠道受理则为进行中_回调成功后为成功并写消息() {
        Long id = transferService.createTransfer(req("TF1", "mock", "acc-1"));
        PayTransferDO transfer = transferMapper.selectById(id);
        assertThat(transfer.getStatus()).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
        assertThat(fakeProvider.client().lastTransferReq.getNotifyUrl()).endsWith("/app-api/pay/notify/transfer/" + channel.getId());

        transferService.notifyTransfer(channel, PayTransferResult.success(transfer.getNo(), "CT", 200, LocalDateTime.now(), "{}"));
        assertThat(transferMapper.selectById(id).getStatus()).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
        verify(outboxMessageService).saveAndPublish(eq(PayTransferMessage.TOPIC_SUCCESS), any());
    }

    @Test
    void 重复发起返回原单_金额或账号不同报错() {
        Long id = transferService.createTransfer(req("TF2", "mock", "acc-1"));
        assertThat(transferService.createTransfer(req("TF2", "mock", "acc-1"))).isEqualTo(id);
        assertServiceException(() -> transferService.createTransfer(req("TF2", "mock", "acc-2")), PAY_TRANSFER_EXISTS_MISMATCH);
        assertServiceException(() -> transferService.createTransfer(req("TF2", "mock", "acc-1").setPrice(201)), PAY_TRANSFER_EXISTS_MISMATCH);
        assertThat(fakeProvider.client().unifiedTransferCalls).hasValue(1);
    }

    @Test
    void 钱包转账的收款账号须为会员编号() {
        assertServiceException(() -> transferService.createTransfer(req("TF3", "wallet", "abc")), PAY_TRANSFER_WALLET_ACCOUNT_INVALID);
    }

    @Test
    void 钱包转账的收款账号须为正整数() {
        assertServiceException(() -> transferService.createTransfer(req("TF3-0", "wallet", "0")), PAY_TRANSFER_WALLET_ACCOUNT_INVALID);
        assertServiceException(() -> transferService.createTransfer(req("TF3-1", "wallet", "-5")), PAY_TRANSFER_WALLET_ACCOUNT_INVALID);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_transfer WHERE app_id = ?", Integer.class, app.getId())).isZero();
    }

    @Test
    void 回调失败后为失败并写消息_终态后再回调不改变() {
        Long id = transferService.createTransfer(req("TF4", "mock", "acc-1"));
        String no = transferMapper.selectById(id).getNo();
        transferService.notifyTransfer(channel, PayTransferResult.failure(no, "E", "账户异常", "{}"));
        transferService.notifyTransfer(channel, PayTransferResult.success(no, "CT", 200, LocalDateTime.now(), "{}"));
        assertThat(transferMapper.selectById(id).getStatus()).isEqualTo(PayTransferStatusEnum.FAILURE.getStatus());
        verify(outboxMessageService).saveAndPublish(eq(PayTransferMessage.TOPIC_FAILURE), any());
        verify(outboxMessageService, never()).saveAndPublish(eq(PayTransferMessage.TOPIC_SUCCESS), any());
    }

    @Test
    void 补查_等待中重发_进行中查询() {
        fakeProvider.client().throwOnCall = new IllegalStateException("timeout");
        Long id = transferService.createTransfer(req("TF5", "mock", "acc-1"));
        assertThat(transferMapper.selectById(id).getStatus()).isEqualTo(PayTransferStatusEnum.WAITING.getStatus());
        fakeProvider.client().throwOnCall = null;
        jdbcTemplate.update("UPDATE pay_transfer SET create_time = ? WHERE id = ?", LocalDateTime.now().minusMinutes(5), id);

        transferService.syncTransfer(100);
        assertThat(transferMapper.selectById(id).getStatus()).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());

        fakeProvider.client().onGetTransfer = no -> PayTransferResult.success(no, "CT", 200, LocalDateTime.now(), "{}");
        transferService.syncTransfer(100);
        assertThat(transferMapper.selectById(id).getStatus()).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
    }

    @Test
    void 渠道同步返回成功时立即完成并写消息() {
        fakeProvider.client().onUnifiedTransfer = req -> success(req.getOutTransferNo());
        Long id = transferService.createTransfer(req("TF6", "mock", "acc-1"));
        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
        verify(outboxMessageService).saveAndPublish(eq(PayTransferMessage.TOPIC_SUCCESS), any());
    }

    @Test
    void 状态更新的条件在SQL里按当前行判断() {
        Long id = transferService.createTransfer(req("TF26", "mock", "acc-1"));
        PayTransferDO update = new PayTransferDO().setStatus(PayTransferStatusEnum.FAILURE.getStatus());

        // 当前为进行中：期望状态为终态的更新不生效，期望未完成的更新生效，之后再按未完成更新则不再生效
        assertThat(transferMapper.updateByIdAndStatus(id, List.of(PayTransferStatusEnum.SUCCESS.getStatus()), update)).isZero();
        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
        List<Integer> unfinished = List.of(PayTransferStatusEnum.WAITING.getStatus(), PayTransferStatusEnum.PROCESSING.getStatus());
        assertThat(transferMapper.updateByIdAndStatus(id, unfinished, update)).isEqualTo(1);
        assertThat(transferMapper.updateByIdAndStatus(id, unfinished, update)).isZero();
    }

    @Test
    void 回调成功金额与转账单不一致时拒绝且不改状态不写消息() {
        Long id = transferService.createTransfer(req("TF28", "mock", "acc-1"));
        String no = transferMapper.selectById(id).getNo();

        assertServiceException(() -> transferService.notifyTransfer(channel,
                PayTransferResult.success(no, "CT", 1, LocalDateTime.now(), "{}")), PAY_TRANSFER_NOTIFY_PRICE_MISMATCH);

        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
        verify(outboxMessageService, never()).saveAndPublish(any(), any());
    }

    @Test
    void 回调成功不带金额时不核对金额() {
        Long id = transferService.createTransfer(req("TF29", "mock", "acc-1"));
        String no = transferMapper.selectById(id).getNo();

        transferService.notifyTransfer(channel, PayTransferResult.success(no, "CT", null, LocalDateTime.now(), "{}"));

        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
    }

    @Test
    void 渠道同步返回成功后_随后返回的等待不能把转账改回进行中() {
        // 发起调用返回之前，结果回调已先到并完成了转账：进行中的置位必须是对"仍为等待"的条件更新
        fakeProvider.client().onUnifiedTransfer = req -> {
            transferService.notifyTransfer(channel, success(req.getOutTransferNo()));
            return PayTransferResult.waiting(req.getOutTransferNo());
        };

        Long id = transferService.createTransfer(req("TF30", "mock", "acc-1"));

        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
        verify(outboxMessageService).saveAndPublish(eq(PayTransferMessage.TOPIC_SUCCESS), any());
    }

    @Test
    void 补查条数上限小于1时直接返回_不查询() {
        processingTransfer("TF31");
        doThrow(new AssertionError("limit 小于 1 不应查询")).when(transferMapper)
                .selectListByStatusAndCreateTimeLt(any(), any(), any(), anyInt());

        assertThat(transferService.syncTransfer(0)).isZero();
        assertThat(transferService.syncTransfer(-1)).isZero();
    }

    // ---------- 渠道绑定 ----------

    @Test
    void 通知渠道与转账单所属渠道不一致时拒绝且不改状态不写消息() {
        Long id = transferService.createTransfer(req("TF7", "mock", "acc-1"));
        String no = transferMapper.selectById(id).getNo();
        PayChannelDO other = new PayChannelDO().setAppId(app.getId()).setCode("wx_lite").setStatus(0).setFeeRate(0D).setConfig("{}");
        channelMapper.insert(other);

        assertServiceException(() -> transferService.notifyTransfer(other, success(no)), PAY_TRANSFER_NOT_FOUND);
        assertServiceException(() -> transferService.notifyTransfer(other,
                PayTransferResult.failure(no, "E", "x", "{}")), PAY_TRANSFER_NOT_FOUND);

        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
        verify(outboxMessageService, never()).saveAndPublish(eq(PayTransferMessage.TOPIC_SUCCESS), any());
        verify(outboxMessageService, never()).saveAndPublish(eq(PayTransferMessage.TOPIC_FAILURE), any());
    }

    @Test
    void 通知不存在的转账单报错() {
        assertServiceException(() -> transferService.notifyTransfer(channel, success("T-NOT-EXISTS")), PAY_TRANSFER_NOT_FOUND);
    }

    // ---------- 消息的租户 ----------

    @Test
    void 消息的租户取转账单所属租户_而不是当前线程的租户() {
        Long id = transferService.createTransfer(req("TF8", "mock", "acc-1"));
        String no = transferMapper.selectById(id).getNo();

        // 线程上没有租户上下文（如跨租户的任务）：消息仍须带上转账单所属的租户
        TenantContextHolder.clear();
        TenantUtils.executeIgnore(() -> transferService.notifyTransfer(channel, success(no)));

        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(outboxMessageService).saveAndPublish(eq(PayTransferMessage.TOPIC_SUCCESS), message.capture());
        assertThat(((PayTransferMessage) message.getValue()).getTenantId()).isEqualTo(TENANT_ID);
    }

    // ---------- 重复的商户转账单号 ----------

    @Test
    void 并发重复发起同一商户转账单号_后到者拿到原转账单而不是异常() {
        Long first = transferService.createTransfer(req("TF9", "mock", "acc-1"));
        // 模拟后到者在先到者提交之前查过一次、没查到：它随后插入时撞上唯一键
        doReturn(null).doCallRealMethod().when(transferMapper).selectByAppIdAndMerchantTransferId(any(), eq("TF9"));

        assertThat(transferService.createTransfer(req("TF9", "mock", "acc-1"))).isEqualTo(first);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_transfer WHERE app_id = ?", Integer.class, app.getId())).isEqualTo(1);
        assertThat(fakeProvider.client().unifiedTransferCalls).as("后到者不重复调用渠道").hasValue(1);
    }

    @Test
    void 并发重复发起同一商户转账单号但金额不同_后到者报不一致() {
        transferService.createTransfer(req("TF10", "mock", "acc-1"));
        doReturn(null).doCallRealMethod().when(transferMapper).selectByAppIdAndMerchantTransferId(any(), eq("TF10"));

        assertServiceException(() -> transferService.createTransfer(req("TF10", "mock", "acc-1").setPrice(201)), PAY_TRANSFER_EXISTS_MISMATCH);
    }

    @Test
    void 在外层事务中撞上唯一键时原样抛出() {
        transferService.createTransfer(req("TF11", "mock", "acc-1"));
        doReturn(null).doCallRealMethod().when(transferMapper).selectByAppIdAndMerchantTransferId(any(), eq("TF11"));

        // 外层事务（可重复读）的快照里不一定看得到先到者：不再重读，由外层调用方处理
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> transferService.createTransfer(req("TF11", "mock", "acc-1"))))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void 撞上唯一键但重读不到记录时原样抛出() {
        transferService.createTransfer(req("TF12", "mock", "acc-1"));
        doReturn(null).when(transferMapper).selectByAppIdAndMerchantTransferId(any(), eq("TF12"));

        assertThatThrownBy(() -> transferService.createTransfer(req("TF12", "mock", "acc-1")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    // ---------- 渠道调用不在调用方的事务内 ----------

    // 本用例的外层事务由测试管理、结束时回滚，对 PayAfterCommitExecutor 的回归并不敏感；真正守住"提交后才调渠道"的是下面几个让外层事务真正提交的用例
    @Test
    @Transactional
    void 在外层事务中发起时_渠道调用推迟到外层提交之后() {
        fakeProvider.client().lastTransferReq = null;
        transferService.createTransfer(req("TF13", "mock", "acc-1"));
        assertThat(fakeProvider.client().lastTransferReq).isNull();
    }

    @Test
    void 外层事务提交后才调用渠道_同步返回的结果在提交后落库() {
        fakeProvider.client().onUnifiedTransfer = req -> success(req.getOutTransferNo());
        AtomicInteger callsBeforeCommit = new AtomicInteger(-1);
        Long id = new TransactionTemplate(transactionManager).execute(status -> {
            Long created = transferService.createTransfer(req("TF14", "mock", "acc-1"));
            callsBeforeCommit.set(fakeProvider.client().unifiedTransferCalls.get());
            return created;
        });
        assertThat(callsBeforeCommit.get()).as("外层提交前不调用渠道").isZero();
        assertThat(fakeProvider.client().unifiedTransferCalls).as("外层提交后调用渠道").hasValue(1);
        // 提交后触发的回写是独立事务：不能随已完成的外层事务一同丢失
        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
    }

    @Test
    void 外层事务提交后调用渠道时不处于任何事务中_渠道自带的本地事务才会真正提交() {
        AtomicBoolean inTransaction = new AtomicBoolean(true);
        fakeProvider.client().onUnifiedTransfer = req -> {
            inTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            return PayTransferResult.waiting(req.getOutTransferNo());
        };
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> transferService.createTransfer(req("TF15", "mock", "acc-1")));
        assertThat(fakeProvider.client().lastTransferReq).isNotNull();
        assertThat(inTransaction.get()).as("渠道调用时不应有事务（钱包渠道的本地事务会误加入已完成的外层事务）").isFalse();
    }

    @Test
    void 外层事务回滚时不创建转账单也不调用渠道() {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                transferService.createTransfer(req("TF16", "mock", "acc-1"));
                throw new IllegalStateException("外层失败");
            });
        } catch (IllegalStateException expected) {
            // 外层事务回滚
        }
        assertThat(fakeProvider.client().lastTransferReq).isNull();
        assertThat(transferMapper.selectByAppIdAndMerchantTransferId(app.getId(), "TF16")).isNull();
    }

    @Test
    void 提交后的渠道调用在发起时的租户下执行() {
        AtomicReference<Long> tenantAtCall = new AtomicReference<>(-1L);
        fakeProvider.client().onUnifiedTransfer = req -> {
            tenantAtCall.set(TenantContextHolder.getTenantId());
            return PayTransferResult.waiting(req.getOutTransferNo());
        };
        // 租户上下文只包住发起，外层事务在它之外提交：提交时线程上已没有租户
        TenantContextHolder.clear();
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> TenantUtils.execute(TENANT_ID, () -> transferService.createTransfer(req("TF17", "mock", "acc-1"))));
        assertThat(tenantAtCall.get()).isEqualTo(TENANT_ID);
    }

    @Test
    void 提交后发起渠道转账时出现任何异常都不外传_转账保持等待() {
        // 渠道调用之外的环节出错（这里是读转账单失败）：转账单已落库，不能让发起方收到异常
        doThrow(new IllegalStateException("db down")).when(transferMapper).selectById(ArgumentMatchers.<Serializable>any());

        Long id = new TransactionTemplate(transactionManager).execute(
                status -> transferService.createTransfer(req("TF18", "mock", "acc-1")));
        Mockito.reset(transferMapper);

        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.WAITING.getStatus());
    }

    @Test
    void 提交后任务抛出Error也不外传_转账保持等待() {
        fakeProvider.client().onUnifiedTransfer = req -> {
            throw new NoClassDefFoundError("渠道依赖缺失");
        };

        Long id = transferService.createTransfer(req("TF27", "mock", "acc-1"));

        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.WAITING.getStatus());
    }

    @Test
    void 渠道调用异常时转账保持等待() {
        fakeProvider.client().throwOnCall = new IllegalStateException("timeout");
        Long id = transferService.createTransfer(req("TF19", "mock", "acc-1"));
        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.WAITING.getStatus());
    }

    // ---------- 渠道同步结果回写失败 ----------

    @Test
    void 渠道同步返回的结果回写失败时转账保持等待_发起方仍拿到转账单_补查后完成() {
        fakeProvider.client().onUnifiedTransfer = req -> success(req.getOutTransferNo());
        // 与状态变更同一事务写消息：消息写失败则状态变更一并回滚
        doThrow(new IllegalStateException("outbox down")).when(outboxMessageService).saveAndPublish(any(), any());

        Logger logger = (Logger) LoggerFactory.getLogger(PayTransferServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        Long id;
        try {
            id = transferService.createTransfer(req("TF20", "mock", "acc-1"));
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(id).isNotNull();
        assertThat(appender.list).as("回写失败只记警告日志，由 invokeTransfer 自己处理").anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(ch.qos.logback.classic.Level.WARN);
            assertThat(e.getFormattedMessage()).contains("回写失败");
        });
        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.WAITING.getStatus());

        Mockito.reset(outboxMessageService);
        backdate(id);
        transferService.syncTransfer(100);
        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
        verify(outboxMessageService).saveAndPublish(eq(PayTransferMessage.TOPIC_SUCCESS), any());
    }

    // ---------- 补查 ----------

    @Test
    void 补查_每批一条时前一条查询异常不影响后一条() {
        Long failing = processingTransfer("TF21");
        Long ok = processingTransfer("TF22");
        String failingNo = transferMapper.selectById(failing).getNo();
        AtomicInteger failingQueries = new AtomicInteger();
        fakeProvider.client().onGetTransfer = no -> {
            if (no.equals(failingNo)) {
                // 游标回归时同一条会被反复查询：用 Error 越过 syncTransfer 的 catch (Exception) 立即失败，而不是无限循环
                if (failingQueries.incrementAndGet() > 1) {
                    throw new AssertionError("同一条转账被重复查询，游标没有向前推进");
                }
                throw new IllegalStateException("timeout");
            }
            return success(no);
        };

        int count = transferService.syncTransfer(1);

        assertThat(statusOf(failing)).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
        assertThat(statusOf(ok)).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void 补查_所有转账都失败时每条只尝试一次并结束() {
        processingTransfer("TF23");
        AtomicInteger queries = new AtomicInteger();
        fakeProvider.client().onGetTransfer = no -> {
            if (queries.incrementAndGet() > 1) {
                throw new AssertionError("同一条转账被重复查询，游标没有向前推进");
            }
            throw new IllegalStateException("timeout");
        };

        transferService.syncTransfer(1);

        assertThat(queries.get()).isEqualTo(1);
    }

    @Test
    void 补查_条数超过一页时逐页处理完() {
        Long[] ids = new Long[5];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = processingTransfer("TF3" + i);
        }
        fakeProvider.client().onGetTransfer = this::success;

        int count = transferService.syncTransfer(2);

        assertThat(count).isEqualTo(5);
        for (Long id : ids) {
            assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.SUCCESS.getStatus());
        }
    }

    @Test
    void 补查_不处理创建不久的转账() {
        Long id = transferService.createTransfer(req("TF24", "mock", "acc-1"));
        fakeProvider.client().onGetTransfer = this::success;

        assertThat(transferService.syncTransfer(100)).isZero();

        assertThat(statusOf(id)).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
    }

    @Test
    void 补查_渠道已删除时记错误日志并继续() {
        Long orphan = processingTransfer("TF25");
        String orphanNo = transferMapper.selectById(orphan).getNo();
        channelMapper.deleteById(channel.getId());
        Logger logger = (Logger) LoggerFactory.getLogger(PayTransferServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            transferService.syncTransfer(100);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).filteredOn(e -> e.getLevel() == ch.qos.logback.classic.Level.ERROR)
                .anySatisfy(e -> assertThat(e.getFormattedMessage()).contains(orphanNo).contains(channel.getId().toString()));
        assertThat(statusOf(orphan)).isEqualTo(PayTransferStatusEnum.PROCESSING.getStatus());
    }
}
