package com.ccloud.module.pay.service.order;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.number.MoneyUtils;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderPageReqVO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderExtensionDO;
import com.ccloud.module.pay.dal.mysql.order.PayOrderExtensionMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderMapper;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.PayChannelEnum;
import com.ccloud.module.pay.enums.order.PayOrderStatusEnum;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.PayClient;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayClientStatus;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderUnifiedReq;
import com.ccloud.module.pay.mq.message.PayOrderMessage;
import com.ccloud.module.pay.service.app.PayAppService;
import com.ccloud.module.pay.service.channel.PayChannelService;
import com.ccloud.module.pay.service.common.PayJobBudget;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class PayOrderServiceImpl implements PayOrderService {

    /**
     * 发起支付的互斥锁。订单编号全局唯一，键不分租户。有效期须覆盖一次渠道调用，超时由渠道层自身超时兜底
     */
    private static final String SUBMIT_LOCK_KEY = "pay:order:submit:%d";
    private static final Duration SUBMIT_LOCK_TTL = Duration.ofSeconds(30);
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    @Resource
    private PayOrderMapper orderMapper;
    @Resource
    private PayOrderExtensionMapper extensionMapper;
    @Resource
    private PayAppService appService;
    @Resource
    private PayChannelService channelService;
    @Resource
    private PayNoRedisDAO noRedisDAO;
    @Resource
    private OutboxMessageService outboxMessageService;
    @Resource
    private PayProperties payProperties;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private PayClientFactory payClientFactory;

    @Override
    public Long createOrder(PayOrderCreateReqDTO reqDTO) {
        PayAppDO app = appService.validPayApp(reqDTO.getAppKey());
        // 充值支付单（WR 订单）只由钱包充值服务经带应用的重载创建；外部 RPC 不能往充值应用里预创建订单
        if (appService.isRechargeApp(app)) {
            throw exception(PAY_APP_RECHARGE_NOT_ALLOWED);
        }
        return createOrder(app, reqDTO);
    }

    @Override
    public Long createOrder(PayAppDO app, PayOrderCreateReqDTO reqDTO) {
        PayOrderDO existing = orderMapper.selectByAppIdAndMerchantOrderId(app.getId(), reqDTO.getMerchantOrderId());
        if (existing != null) {
            return sameOrder(existing, reqDTO);
        }
        PayOrderDO order = new PayOrderDO().setAppId(app.getId()).setMerchantOrderId(reqDTO.getMerchantOrderId())
                .setSubject(reqDTO.getSubject()).setBody(StrUtil.nullToEmpty(reqDTO.getBody())).setPrice(reqDTO.getPrice())
                .setExpireTime(reqDTO.getExpireTime()).setUserIp(reqDTO.getUserIp())
                .setUserId(reqDTO.getUserId()).setUserType(reqDTO.getUserType())
                .setStatus(PayOrderStatusEnum.WAITING.getStatus()).setRefundPrice(0).setChannelFeePrice(0);
        try {
            orderMapper.insert(order);
            return order.getId();
        } catch (DuplicateKeyException e) {
            // 并发重复创建：以先写入的为准
            return sameOrder(orderMapper.selectByAppIdAndMerchantOrderId(app.getId(), reqDTO.getMerchantOrderId()), reqDTO);
        }
    }

    /**
     * 同一商户订单号重复创建：金额一致返回原单，不一致说明业务方用同一单号改了价，拒绝
     */
    private static Long sameOrder(PayOrderDO existing, PayOrderCreateReqDTO reqDTO) {
        if (!existing.getPrice().equals(reqDTO.getPrice())) {
            throw exception(PAY_ORDER_EXISTS_PRICE_MISMATCH);
        }
        return existing.getId();
    }

    /**
     * 不加事务：渠道调用必须在数据库事务之外（钱包渠道自带本地事务）。
     *
     * <p>同一订单的发起支付串行执行：每次发起都会生成各自的支付尝试（外部单号不同），钱包流水的唯一键拦不住并发的重复扣款，
     * 只能先互斥，再在锁内重新校验订单状态
     */
    @Override
    public PayOrderSubmitRespVO submitOrder(Long orderId, PayOrderSubmitReqVO reqVO, String userIp) {
        String lockKey = String.format(SUBMIT_LOCK_KEY, orderId);
        String lockToken = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, SUBMIT_LOCK_TTL))) {
            throw exception(PAY_ORDER_SUBMIT_IN_PROGRESS);
        }
        try {
            return doSubmitOrder(orderId, reqVO, userIp);
        } finally {
            // 仅释放自己持有的锁：超过有效期被他人接手后，不能误删对方的锁
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockToken);
        }
    }

    private PayOrderSubmitRespVO doSubmitOrder(Long orderId, PayOrderSubmitReqVO reqVO, String userIp) {
        PayOrderDO order = validateOrderCanSubmit(orderId);
        PayChannelDO channel = channelService.validPayChannel(order.getAppId(), reqVO.getChannelCode());
        if (PayChannelEnum.WALLET.getCode().equals(channel.getCode())) {
            if (order.getUserId() == null || !UserTypeEnum.MEMBER.getValue().equals(order.getUserType())) {
                throw exception(PAY_ORDER_WALLET_REQUIRE_MEMBER);
            }
            if (appService.isRechargeApp(appService.getApp(order.getAppId()))) {
                throw exception(PAY_ORDER_RECHARGE_NOT_ALLOW_WALLET);
            }
        }
        // 先取渠道客户端再落库：渠道编码已不受支持（如遗留的 Mock 渠道）时直接拒绝，不留下无人能补查的支付尝试
        PayClient payClient = channelService.getPayClient(channel);
        // 支付尝试先落库：渠道调用结果未知时，补查任务凭它向渠道查单
        PayOrderExtensionDO ext = new PayOrderExtensionDO().setNo(noRedisDAO.generate(PayNoRedisDAO.ORDER_PREFIX))
                .setOrderId(order.getId()).setChannelId(channel.getId()).setChannelCode(channel.getCode())
                .setPayMethod(reqVO.getPayMethod()).setUserIp(userIp).setChannelExtras(reqVO.getChannelExtras())
                .setStatus(PayOrderStatusEnum.WAITING.getStatus());
        extensionMapper.insert(ext);

        PayOrderResult result;
        try {
            result = payClient.unifiedOrder(new PayOrderUnifiedReq()
                    .setOutTradeNo(ext.getNo()).setSubject(order.getSubject()).setBody(order.getBody())
                    .setPrice(order.getPrice()).setExpireTime(order.getExpireTime())
                    .setNotifyUrl(channelService.buildNotifyUrl("order", channel.getId())).setUserIp(userIp)
                    .setPayMethod(reqVO.getPayMethod()).setUserId(order.getUserId()).setUserType(order.getUserType())
                    .setChannelExtras(reqVO.getChannelExtras()));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[submitOrder] 支付尝试 {} 调用渠道异常，结果未知，等待补查", ext.getNo(), e);
            throw exception(PAY_ORDER_SUBMIT_CHANNEL_ERROR, "渠道暂时不可用，请稍后查询支付结果");
        }
        if (result.getStatus() != PayClientStatus.WAITING) {
            notifyOrder(channel, result);
        }
        if (result.getStatus() == PayClientStatus.FAILURE) {
            throw exception(PAY_ORDER_SUBMIT_CHANNEL_ERROR, StrUtil.blankToDefault(result.getErrorMsg(), "渠道返回失败"));
        }
        return new PayOrderSubmitRespVO().setStatus(orderMapper.selectById(orderId).getStatus()).setNo(ext.getNo())
                .setDisplayMode(result.getDisplayMode() == null ? null : result.getDisplayMode().name())
                .setDisplayContent(result.getDisplayContent());
    }

    private PayOrderDO validateOrderCanSubmit(Long orderId) {
        PayOrderDO order = orderMapper.selectById(orderId);
        if (order == null) {
            throw exception(PAY_ORDER_NOT_FOUND);
        }
        if (!PayOrderStatusEnum.WAITING.getStatus().equals(order.getStatus())) {
            throw exception(PAY_ORDER_STATUS_IS_NOT_WAITING);
        }
        if (order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw exception(PAY_ORDER_IS_EXPIRED);
        }
        return order;
    }

    /**
     * 回调、同步结果、补查共用。自带事务（经 TransactionTemplate），本类内部调用也生效
     */
    @Override
    public void notifyOrder(PayChannelDO channel, PayOrderResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            PayOrderExtensionDO ext = extensionMapper.selectByNo(result.getOutTradeNo());
            if (ext == null) {
                throw exception(PAY_ORDER_EXTENSION_NOT_FOUND);
            }
            // 回调须来自发起该支付尝试的渠道：用渠道 A 的密钥验过签的回调，不能完成渠道 B 上的支付尝试
            if (!Objects.equals(ext.getChannelId(), channel.getId())) {
                log.error("[notifyOrder] 支付尝试 {} 属于渠道 {}，却由渠道 {} 通知，拒绝", ext.getNo(), ext.getChannelId(), channel.getId());
                throw exception(PAY_ORDER_EXTENSION_NOT_FOUND);
            }
            if (result.getStatus() == PayClientStatus.SUCCESS) {
                notifyOrderSuccess(channel, ext, result);
            } else if (result.getStatus() == PayClientStatus.FAILURE) {
                notifyOrderClosed(ext, result);
            }
        });
    }

    private void notifyOrderSuccess(PayChannelDO channel, PayOrderExtensionDO ext, PayOrderResult result) {
        if (PayOrderStatusEnum.SUCCESS.getStatus().equals(ext.getStatus())) {
            return;
        }
        PayOrderDO order = orderMapper.selectById(ext.getOrderId());
        if (!Objects.equals(result.getPrice(), order.getPrice())) {
            log.error("[notifyOrderSuccess] 支付尝试 {} 渠道金额 {} 与订单金额 {} 不一致，拒绝", ext.getNo(), result.getPrice(), order.getPrice());
            throw exception(PAY_ORDER_NOTIFY_PRICE_MISMATCH);
        }
        // 关闭的支付尝试也接受成功：关单后用户才付款，钱已到账，必须记下来。
        // 条件是"尚未成功"而不是读到的状态：读取后超时关单可能把等待改成了关闭，按旧状态更新会丢掉这笔已到账的支付；
        // 并发的另一笔通知会在行锁上等待，待其提交后看到已成功，影响 0 行，保证只入账一次
        int updated = extensionMapper.updateByIdAndStatusNot(ext.getId(), PayOrderStatusEnum.SUCCESS.getStatus(), new PayOrderExtensionDO()
                .setStatus(PayOrderStatusEnum.SUCCESS.getStatus())
                .setChannelNotifyData(StrUtil.maxLength(result.getRawData(), 4000)));
        if (updated == 0) {
            return;
        }
        PayOrderDO update = new PayOrderDO().setStatus(PayOrderStatusEnum.SUCCESS.getStatus())
                .setChannelId(channel.getId()).setChannelCode(channel.getCode()).setPayMethod(ext.getPayMethod())
                .setSuccessTime(ObjectUtil.defaultIfNull(result.getSuccessTime(), LocalDateTime.now()))
                .setExtensionId(ext.getId()).setNo(ext.getNo())
                .setChannelOrderNo(result.getChannelOrderNo()).setChannelUserId(result.getChannelUserId())
                .setChannelFeeRate(channel.getFeeRate())
                .setChannelFeePrice(MoneyUtils.calculateRatePrice(order.getPrice(), channel.getFeeRate()));
        if (orderMapper.updateByIdAndStatus(order.getId(), PayOrderStatusEnum.WAITING.getStatus(), update) == 0) {
            log.error("[notifyOrderSuccess] 订单 {} 已不是待支付，支付尝试 {} 记为异常支付，需人工处理", order.getId(), ext.getNo());
            return;
        }
        PayAppDO app = appService.getApp(order.getAppId());
        outboxMessageService.saveAndPublish(PayOrderMessage.TOPIC_PAID, new PayOrderMessage()
                .setTenantId(order.getTenantId()).setAppId(app.getId()).setAppKey(app.getAppKey())
                .setOrderId(order.getId()).setOrderNo(ext.getNo()).setMerchantOrderId(order.getMerchantOrderId())
                .setPrice(order.getPrice()).setChannelCode(channel.getCode()).setPayMethod(ext.getPayMethod())
                .setSuccessTime(update.getSuccessTime()).setUserId(order.getUserId()).setUserType(order.getUserType()));
    }

    private void notifyOrderClosed(PayOrderExtensionDO ext, PayOrderResult result) {
        extensionMapper.updateByIdAndStatus(ext.getId(), PayOrderStatusEnum.WAITING.getStatus(), new PayOrderExtensionDO()
                .setStatus(PayOrderStatusEnum.CLOSED.getStatus()).setChannelErrorCode(result.getErrorCode())
                .setChannelErrorMsg(StrUtil.maxLength(result.getErrorMsg(), 250))
                .setChannelNotifyData(StrUtil.maxLength(result.getRawData(), 4000)));
    }

    @Override
    public int syncOrder(int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minus(payProperties.getJob().getSyncWindow());
        LocalDateTime to = now.minus(payProperties.getJob().getSyncDelay());
        PayJobBudget budget = PayJobBudget.start(payProperties.getJob().getTimeBudget());
        int count = 0;
        long afterId = 0L;
        List<PayOrderExtensionDO> page;
        // 按编号游标逐页处理：某条长期查询失败时，不会占住队首、饿死后面的记录
        do {
            if (budget.exhausted()) {
                log.warn("[syncOrder] 时间预算用完或线程被中断，本轮已补查 {} 笔，余下留给下一轮", count);
                break;
            }
            long cursor = afterId;
            page = TenantUtils.executeIgnore(() -> extensionMapper.selectListByStatusAndCreateTimeBetween(
                    PayOrderStatusEnum.WAITING.getStatus(), from, to, cursor, limit));
            for (PayOrderExtensionDO ext : page) {
                if (budget.exhausted()) {
                    log.warn("[syncOrder] 时间预算用完或线程被中断，本轮已补查 {} 笔，余下留给下一轮", count);
                    return count;
                }
                afterId = ext.getId();
                try {
                    TenantUtils.execute(ext.getTenantId(), () -> syncExtension(ext));
                    count++;
                } catch (Exception e) {
                    if (PayJobBudget.isInterruption(e)) {
                        Thread.currentThread().interrupt();
                        log.warn("[syncOrder] 支付尝试 {} 补查时线程被中断，结束本轮", ext.getNo());
                        return count;
                    }
                    log.warn("[syncOrder] 支付尝试 {} 补查失败", ext.getNo(), e);
                }
            }
        } while (page.size() >= limit);
        return count;
    }

    private void syncExtension(PayOrderExtensionDO ext) {
        PayChannelDO channel = channelService.getChannel(ext.getChannelId());
        // 渠道已删除，或编码已不受支持（如遗留的 Mock 渠道）：无从查询，留给超时关单
        if (channel == null || !payClientFactory.isSupported(channel.getCode())) {
            return;
        }
        PayOrderResult result = channelService.getPayClient(channel).getOrder(ext.getNo());
        if (result.getStatus() != PayClientStatus.WAITING) {
            notifyOrder(channel, result);
        }
    }

    @Override
    public int expireOrder(int limit) {
        LocalDateTime now = LocalDateTime.now();
        PayJobBudget budget = PayJobBudget.start(payProperties.getJob().getTimeBudget());
        int closed = 0;
        long afterId = 0L;
        List<PayOrderDO> page;
        // 按编号游标逐页处理：某个订单长期关闭失败时，不会占住队首、饿死后面的订单
        do {
            if (budget.exhausted()) {
                log.warn("[expireOrder] 时间预算用完或线程被中断，本轮已关闭 {} 笔，余下留给下一轮", closed);
                break;
            }
            long cursor = afterId;
            page = TenantUtils.executeIgnore(() -> orderMapper.selectListByStatusAndExpireTimeLt(
                    PayOrderStatusEnum.WAITING.getStatus(), now, cursor, limit));
            for (PayOrderDO order : page) {
                if (budget.exhausted()) {
                    log.warn("[expireOrder] 时间预算用完或线程被中断，本轮已关闭 {} 笔，余下留给下一轮", closed);
                    return closed;
                }
                afterId = order.getId();
                try {
                    if (Boolean.TRUE.equals(TenantUtils.execute(order.getTenantId(), () -> expireOne(order)))) {
                        closed++;
                    }
                } catch (Exception e) {
                    if (PayJobBudget.isInterruption(e)) {
                        Thread.currentThread().interrupt();
                        log.warn("[expireOrder] 订单 {} 关闭时线程被中断，结束本轮", order.getId());
                        return closed;
                    }
                    log.warn("[expireOrder] 订单 {} 关闭失败", order.getId(), e);
                }
            }
        } while (page.size() >= limit);
        return closed;
    }

    /**
     * 先向渠道确认每个等待中的支付尝试都未支付，才关闭订单；查询出错则本次跳过，下次再试
     */
    private boolean expireOne(PayOrderDO order) {
        for (PayOrderExtensionDO ext : extensionMapper.selectListByOrderIdAndStatus(order.getId(), PayOrderStatusEnum.WAITING.getStatus())) {
            PayChannelDO channel = channelService.getChannel(ext.getChannelId());
            // 渠道已删除，或编码已不受支持（如遗留的 Mock 渠道）：无从向渠道确认，只能直接关闭，否则订单永远关不掉
            if (channel != null && payClientFactory.isSupported(channel.getCode())) {
                PayOrderResult result = channelService.getPayClient(channel).getOrder(ext.getNo());
                if (result.getStatus() == PayClientStatus.SUCCESS) {
                    notifyOrder(channel, result);
                    return false;
                }
            }
            extensionMapper.updateByIdAndStatus(ext.getId(), PayOrderStatusEnum.WAITING.getStatus(), new PayOrderExtensionDO()
                    .setStatus(PayOrderStatusEnum.CLOSED.getStatus()).setChannelErrorMsg("订单超时关闭"));
        }
        return orderMapper.updateByIdAndStatus(order.getId(), PayOrderStatusEnum.WAITING.getStatus(),
                new PayOrderDO().setStatus(PayOrderStatusEnum.CLOSED.getStatus())) > 0;
    }

    @Override
    public PayOrderDO getOrder(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public PayOrderDO getOrder(Long appId, String merchantOrderId) {
        return orderMapper.selectByAppIdAndMerchantOrderId(appId, merchantOrderId);
    }

    @Override
    public PayOrderExtensionDO getOrderExtension(Long id) {
        return extensionMapper.selectById(id);
    }

    @Override
    public PayOrderExtensionDO getOrderExtensionByNoIgnoreTenant(String no) {
        return TenantUtils.executeIgnore(() -> extensionMapper.selectByNo(no));
    }

    @Override
    public List<PayOrderExtensionDO> getOrderExtensionListByOrderId(Long orderId) {
        return extensionMapper.selectListByOrderId(orderId);
    }

    @Override
    public PageResult<PayOrderDO> getOrderPage(PayOrderPageReqVO reqVO) {
        return orderMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<PayOrderExtensionDO> getAbnormalExtensionPage(PageParam pageParam) {
        return extensionMapper.selectAbnormalPage(pageParam);
    }
}
