package com.ccloud.module.pay.service.refund;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.ccloud.module.pay.controller.admin.refund.vo.PayRefundPageReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.dal.dataobject.refund.PayRefundDO;
import com.ccloud.module.pay.dal.mysql.order.PayOrderMapper;
import com.ccloud.module.pay.dal.mysql.refund.PayRefundMapper;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.order.PayOrderStatusEnum;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayClientStatus;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundUnifiedReq;
import com.ccloud.module.pay.mq.message.PayRefundMessage;
import com.ccloud.module.pay.service.app.PayAppService;
import com.ccloud.module.pay.service.channel.PayChannelService;
import com.ccloud.module.pay.service.common.PayAfterCommitExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.ccloud.framework.common.exception.ServiceException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class PayRefundServiceImpl implements PayRefundService {

    @Resource
    private PayRefundMapper refundMapper;
    @Resource
    private PayOrderMapper orderMapper;
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
    private PayAfterCommitExecutor afterCommitExecutor;

    private record CreateResult(Long refundId, boolean created) {
    }

    @Override
    public Long createRefund(PayRefundCreateReqDTO reqDTO) {
        return createRefund(appService.validPayApp(reqDTO.getAppKey()), reqDTO);
    }

    @Override
    public Long createRefund(PayAppDO app, PayRefundCreateReqDTO reqDTO) {
        CreateResult result;
        try {
            result = transactionTemplate.execute(status -> doCreateRefund(app, reqDTO));
        } catch (DuplicateKeyException e) {
            // 并发重复创建：本事务（含预占额度）已回滚，以先写入的为准
            result = findExistingAfterRace(app, reqDTO, e);
        } catch (ServiceException e) {
            // 后到者在先到者提交前通过了重复校验，随后因先到者占用了额度/处理中而被拒绝：预占是对最新已提交行的更新，
            // 看得到先到者的提交；此时同一商户退款单号的记录已存在，应返回它而不是额度错误
            if (!PAY_REFUND_PRICE_EXCEED.getCode().equals(e.getCode()) && !PAY_REFUND_HAS_REFUNDING.getCode().equals(e.getCode())) {
                throw e;
            }
            result = findExistingAfterRace(app, reqDTO, e);
        }
        CreateResult created = result;
        if (created.created()) {
            // 渠道调用推迟到外层事务提交之后，保留发起时的租户，任何异常都不外抛（退款已落库，结果由补查任务确定）
            afterCommitExecutor.execute("退款 " + created.refundId(), () -> invokeRefund(created.refundId()));
        }
        return result.refundId();
    }

    /**
     * 独立事务路径下重新读取同一商户退款单号的记录（事务已结束，读到的是已提交的行）。没有记录或处于外层事务中（无法确定）时重新抛出原异常
     */
    private CreateResult findExistingAfterRace(PayAppDO app, PayRefundCreateReqDTO reqDTO, RuntimeException cause) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 在外层事务中：外层事务已被标记回滚，由外层调用方处理
            throw cause;
        }
        PayOrderDO order = orderMapper.selectByAppIdAndMerchantOrderId(app.getId(), reqDTO.getMerchantOrderId());
        PayRefundDO existing = refundMapper.selectByAppIdAndMerchantRefundId(app.getId(), reqDTO.getMerchantRefundId());
        if (order == null || existing == null) {
            throw cause;
        }
        return sameRefund(existing, order, reqDTO);
    }

    /**
     * 同一商户退款单号重复创建：订单与金额一致返回原单，否则拒绝
     */
    private static CreateResult sameRefund(PayRefundDO existing, PayOrderDO order, PayRefundCreateReqDTO reqDTO) {
        if (!existing.getOrderId().equals(order.getId()) || !existing.getRefundPrice().equals(reqDTO.getPrice())) {
            throw exception(PAY_REFUND_EXISTS_MISMATCH);
        }
        return new CreateResult(existing.getId(), false);
    }

    private CreateResult doCreateRefund(PayAppDO app, PayRefundCreateReqDTO reqDTO) {
        PayOrderDO order = orderMapper.selectByAppIdAndMerchantOrderId(app.getId(), reqDTO.getMerchantOrderId());
        if (order == null) {
            throw exception(PAY_ORDER_NOT_FOUND);
        }
        PayRefundDO existing = refundMapper.selectByAppIdAndMerchantRefundId(app.getId(), reqDTO.getMerchantRefundId());
        if (existing != null) {
            return sameRefund(existing, order, reqDTO);
        }
        if (!PayOrderStatusEnum.SUCCESS.getStatus().equals(order.getStatus())) {
            throw exception(PayOrderStatusEnum.REFUND.getStatus().equals(order.getStatus())
                    ? PAY_REFUND_PRICE_EXCEED : PAY_REFUND_ORDER_STATUS_ERROR);
        }
        // 软约束，简化对账；金额不超的硬保证在下面的条件更新
        if (refundMapper.selectCountByOrderIdAndStatus(order.getId(), PayRefundStatusEnum.WAITING.getStatus()) > 0) {
            throw exception(PAY_REFUND_HAS_REFUNDING);
        }
        if (orderMapper.reserveRefundPrice(order.getId(), reqDTO.getPrice()) == 0) {
            throw exception(PAY_REFUND_PRICE_EXCEED);
        }
        PayRefundDO refund = new PayRefundDO().setNo(noRedisDAO.generate(PayNoRedisDAO.REFUND_PREFIX)).setAppId(app.getId())
                .setChannelId(order.getChannelId()).setChannelCode(order.getChannelCode()).setOrderId(order.getId())
                .setOrderNo(order.getNo()).setMerchantOrderId(order.getMerchantOrderId())
                .setMerchantRefundId(reqDTO.getMerchantRefundId()).setStatus(PayRefundStatusEnum.WAITING.getStatus())
                .setPayPrice(order.getPrice()).setRefundPrice(reqDTO.getPrice()).setReason(reqDTO.getReason())
                .setUserIp(reqDTO.getUserIp()).setChannelOrderNo(StrUtil.nullToEmpty(order.getChannelOrderNo()));
        refundMapper.insert(refund);
        return new CreateResult(refund.getId(), true);
    }

    /**
     * 向渠道发起退款（按退款单号幂等，可重复调用）。不在事务内；结果在独立事务中回写
     */
    private void invokeRefund(Long refundId) {
        PayRefundDO refund = refundMapper.selectById(refundId);
        if (refund == null || !PayRefundStatusEnum.WAITING.getStatus().equals(refund.getStatus())) {
            return;
        }
        PayChannelDO channel = channelService.getChannel(refund.getChannelId());
        if (channel == null) {
            log.error("[invokeRefund] 退款 {} 的渠道 {} 已删除，无法退款", refund.getNo(), refund.getChannelId());
            return;
        }
        PayRefundResult result;
        try {
            result = channelService.getPayClient(channel).unifiedRefund(new PayRefundUnifiedReq()
                    .setOutTradeNo(refund.getOrderNo()).setOutRefundNo(refund.getNo()).setReason(refund.getReason())
                    .setPayPrice(refund.getPayPrice()).setRefundPrice(refund.getRefundPrice())
                    .setNotifyUrl(channelService.buildNotifyUrl("refund", channel.getId())));
        } catch (Exception e) {
            log.warn("[invokeRefund] 退款 {} 调用渠道异常，等待补查", refund.getNo(), e);
            return;
        }
        if (result.getStatus() != PayClientStatus.WAITING) {
            try {
                notifyRefund(channel, result);
            } catch (Exception e) {
                // 退款已发起并落库，回写失败（如渠道金额不一致）不能让发起方误以为退款没有创建；单据保持等待，由补查再处理
                log.warn("[invokeRefund] 退款 {} 的渠道结果回写失败，等待补查", refund.getNo(), e);
            }
        }
    }

    /**
     * 回调、同步结果、补查共用。自带事务（经 TransactionTemplate），本类内部调用也生效
     */
    @Override
    public void notifyRefund(PayChannelDO channel, PayRefundResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            PayRefundDO refund = refundMapper.selectByNo(result.getOutRefundNo());
            if (refund == null) {
                throw exception(PAY_REFUND_NOT_FOUND);
            }
            // 回调须来自发起该退款的渠道：用渠道 A 的密钥验过签的回调，不能完成渠道 B 上的退款
            if (!Objects.equals(refund.getChannelId(), channel.getId())) {
                log.error("[notifyRefund] 退款 {} 属于渠道 {}，却由渠道 {} 通知，拒绝", refund.getNo(), refund.getChannelId(), channel.getId());
                throw exception(PAY_REFUND_NOT_FOUND);
            }
            if (!PayRefundStatusEnum.WAITING.getStatus().equals(refund.getStatus())) {
                return;
            }
            if (result.getStatus() == PayClientStatus.SUCCESS) {
                notifyRefundSuccess(refund, result);
            } else if (result.getStatus() == PayClientStatus.FAILURE) {
                notifyRefundFailure(refund, result);
            }
        });
    }

    private void notifyRefundSuccess(PayRefundDO refund, PayRefundResult result) {
        if (result.getRefundPrice() != null && !result.getRefundPrice().equals(refund.getRefundPrice())) {
            log.error("[notifyRefundSuccess] 退款 {} 渠道金额 {} 与退款单 {} 不一致，拒绝", refund.getNo(), result.getRefundPrice(), refund.getRefundPrice());
            throw exception(PAY_REFUND_NOTIFY_PRICE_MISMATCH);
        }
        LocalDateTime successTime = ObjectUtil.defaultIfNull(result.getSuccessTime(), LocalDateTime.now());
        if (refundMapper.updateByIdAndStatus(refund.getId(), PayRefundStatusEnum.WAITING.getStatus(), new PayRefundDO()
                .setStatus(PayRefundStatusEnum.SUCCESS.getStatus()).setChannelRefundNo(result.getChannelRefundNo())
                .setSuccessTime(successTime).setChannelNotifyData(StrUtil.maxLength(result.getRawData(), 4000))) == 0) {
            return;
        }
        // 条件在 SQL 里按当前行判断，不用先前读到的快照（并发的其他退款可能刚改过额度）
        orderMapper.updateStatusIfFullyRefunded(refund.getOrderId(), PayOrderStatusEnum.SUCCESS.getStatus(),
                PayOrderStatusEnum.REFUND.getStatus());
        publish(PayRefundMessage.TOPIC_SUCCESS, refund, PayRefundStatusEnum.SUCCESS, successTime, null);
    }

    private void notifyRefundFailure(PayRefundDO refund, PayRefundResult result) {
        if (refundMapper.updateByIdAndStatus(refund.getId(), PayRefundStatusEnum.WAITING.getStatus(), new PayRefundDO()
                .setStatus(PayRefundStatusEnum.FAILURE.getStatus()).setChannelErrorCode(result.getErrorCode())
                .setChannelErrorMsg(StrUtil.maxLength(result.getErrorMsg(), 250))
                .setChannelNotifyData(StrUtil.maxLength(result.getRawData(), 4000))) == 0) {
            return;
        }
        orderMapper.releaseRefundPrice(refund.getOrderId(), refund.getRefundPrice());
        // 预占额度曾使订单看似已退完（其他退款成功时按预占额度判断）：释放后若还有可退额度，订单回到支付成功
        orderMapper.updateStatusIfRefundable(refund.getOrderId(), PayOrderStatusEnum.REFUND.getStatus(),
                PayOrderStatusEnum.SUCCESS.getStatus());
        publish(PayRefundMessage.TOPIC_FAILURE, refund, PayRefundStatusEnum.FAILURE, null, result.getErrorMsg());
    }

    private void publish(String topic, PayRefundDO refund, PayRefundStatusEnum status, LocalDateTime successTime, String errorMsg) {
        PayAppDO app = appService.getApp(refund.getAppId());
        outboxMessageService.saveAndPublish(topic, new PayRefundMessage().setTenantId(refund.getTenantId())
                .setAppId(app.getId()).setAppKey(app.getAppKey()).setRefundId(refund.getId()).setRefundNo(refund.getNo())
                .setOrderId(refund.getOrderId()).setMerchantOrderId(refund.getMerchantOrderId())
                .setMerchantRefundId(refund.getMerchantRefundId()).setRefundPrice(refund.getRefundPrice())
                .setStatus(status.getStatus()).setSuccessTime(successTime).setChannelErrorMsg(errorMsg));
    }

    /**
     * 补查：先查渠道；仍为等待时按同一退款单号重发（渠道按退款单号幂等），覆盖"发起前进程中断"的情况。
     * 按编号游标逐页处理：某条长期查询失败时，不会占住队首、饿死后面的退款
     */
    @Override
    public int syncRefund(int limit) {
        LocalDateTime before = LocalDateTime.now().minus(payProperties.getJob().getSyncDelay());
        int count = 0;
        long afterId = 0L;
        List<PayRefundDO> page;
        do {
            long cursor = afterId;
            page = TenantUtils.executeIgnore(() -> refundMapper.selectListByStatusAndCreateTimeLt(
                    PayRefundStatusEnum.WAITING.getStatus(), before, cursor, limit));
            for (PayRefundDO refund : page) {
                // 先推进游标再处理：失败的退款不会让本轮循环停住或原地打转
                afterId = refund.getId();
                try {
                    TenantUtils.execute(refund.getTenantId(), () -> syncOne(refund));
                    count++;
                } catch (Exception e) {
                    log.warn("[syncRefund] 退款 {} 补查失败", refund.getNo(), e);
                }
            }
        } while (page.size() >= limit);
        return count;
    }

    private void syncOne(PayRefundDO refund) {
        PayChannelDO channel = channelService.getChannel(refund.getChannelId());
        if (channel == null) {
            log.error("[syncRefund] 退款 {} 的渠道 {} 已删除，无法补查", refund.getId(), refund.getChannelId());
            return;
        }
        PayRefundResult result = channelService.getPayClient(channel).getRefund(refund.getOrderNo(), refund.getNo());
        if (result.getStatus() == PayClientStatus.WAITING) {
            invokeRefund(refund.getId());
        } else {
            notifyRefund(channel, result);
        }
    }

    @Override
    public PayRefundDO getRefund(Long id) {
        return refundMapper.selectById(id);
    }

    @Override
    public PayRefundDO getRefund(Long appId, String merchantRefundId) {
        return refundMapper.selectByAppIdAndMerchantRefundId(appId, merchantRefundId);
    }

    @Override
    public PageResult<PayRefundDO> getRefundPage(PayRefundPageReqVO reqVO) {
        return refundMapper.selectPage(reqVO);
    }
}
