package com.ccloud.module.pay.service.transfer;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.api.transfer.dto.PayTransferCreateReqDTO;
import com.ccloud.module.pay.controller.admin.transfer.vo.PayTransferPageReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.transfer.PayTransferDO;
import com.ccloud.module.pay.dal.mysql.transfer.PayTransferMapper;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.PayChannelEnum;
import com.ccloud.module.pay.enums.transfer.PayTransferStatusEnum;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayClientStatus;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferUnifiedReq;
import com.ccloud.module.pay.mq.message.PayTransferMessage;
import com.ccloud.module.pay.service.app.PayAppService;
import com.ccloud.module.pay.service.channel.PayChannelService;
import com.ccloud.module.pay.service.common.PayAfterCommitExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
public class PayTransferServiceImpl implements PayTransferService {

    private static final List<Integer> UNFINISHED = List.of(PayTransferStatusEnum.WAITING.getStatus(),
            PayTransferStatusEnum.PROCESSING.getStatus());

    @Resource
    private PayTransferMapper transferMapper;
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

    @Override
    public Long createTransfer(PayTransferCreateReqDTO reqDTO) {
        PayAppDO app = appService.validPayApp(reqDTO.getAppKey());
        PayTransferDO existing = transferMapper.selectByAppIdAndMerchantTransferId(app.getId(), reqDTO.getMerchantTransferId());
        if (existing != null) {
            return sameTransfer(existing, reqDTO);
        }
        PayChannelDO channel = channelService.validPayChannel(app.getId(), reqDTO.getChannelCode());
        if (PayChannelEnum.WALLET.getCode().equals(channel.getCode()) && !isPositiveLong(reqDTO.getUserAccount())) {
            throw exception(PAY_TRANSFER_WALLET_ACCOUNT_INVALID);
        }
        PayTransferDO transfer = new PayTransferDO().setNo(noRedisDAO.generate(PayNoRedisDAO.TRANSFER_PREFIX))
                .setAppId(app.getId()).setChannelId(channel.getId()).setChannelCode(channel.getCode())
                .setMerchantTransferId(reqDTO.getMerchantTransferId()).setSubject(reqDTO.getSubject())
                .setPrice(reqDTO.getPrice()).setUserAccount(reqDTO.getUserAccount()).setUserName(reqDTO.getUserName())
                .setUserIp(reqDTO.getUserIp()).setStatus(PayTransferStatusEnum.WAITING.getStatus());
        try {
            transferMapper.insert(transfer);
        } catch (DuplicateKeyException e) {
            return findExistingAfterRace(app, reqDTO, e);
        }
        // 渠道调用推迟到外层事务提交之后（没有外层事务则立即进行），保留发起时的租户，任何异常都不外抛（转账单已落库，结果由补查任务确定）
        Long transferId = transfer.getId();
        afterCommitExecutor.execute("转账 " + transfer.getNo(), () -> invokeTransfer(transferId));
        return transferId;
    }

    /**
     * 钱包转账的收款账号须为会员编号：正整数
     */
    private static boolean isPositiveLong(String account) {
        return NumberUtil.isLong(account) && Long.parseLong(account) > 0;
    }

    /**
     * 并发重复创建：独立（无外层事务）路径下重新读取同一商户转账单号的已提交记录，以先写入的为准。
     * 没有记录，或处于外层事务中（可重复读的快照不一定看得到先写入者）时，重新抛出原异常
     */
    private Long findExistingAfterRace(PayAppDO app, PayTransferCreateReqDTO reqDTO, DuplicateKeyException cause) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw cause;
        }
        PayTransferDO existing = transferMapper.selectByAppIdAndMerchantTransferId(app.getId(), reqDTO.getMerchantTransferId());
        if (existing == null) {
            throw cause;
        }
        return sameTransfer(existing, reqDTO);
    }

    /**
     * 同一商户转账单号重复创建：金额与收款账号一致返回原单，否则拒绝
     */
    private static Long sameTransfer(PayTransferDO existing, PayTransferCreateReqDTO reqDTO) {
        if (!existing.getPrice().equals(reqDTO.getPrice()) || !existing.getUserAccount().equals(reqDTO.getUserAccount())) {
            throw exception(PAY_TRANSFER_EXISTS_MISMATCH);
        }
        return existing.getId();
    }

    /**
     * 只对等待中的转账调用渠道（渠道按转账单号幂等，重发安全）；渠道受理但未完成时置为进行中。不在事务内；结果在独立事务中回写
     */
    private void invokeTransfer(Long transferId) {
        PayTransferDO transfer = transferMapper.selectById(transferId);
        if (transfer == null || !PayTransferStatusEnum.WAITING.getStatus().equals(transfer.getStatus())) {
            return;
        }
        PayChannelDO channel = channelService.getChannel(transfer.getChannelId());
        if (channel == null) {
            log.error("[invokeTransfer] 转账 {} 的渠道 {} 已删除，无法转账", transfer.getNo(), transfer.getChannelId());
            return;
        }
        PayTransferResult result;
        try {
            result = channelService.getPayClient(channel).unifiedTransfer(new PayTransferUnifiedReq()
                    .setOutTransferNo(transfer.getNo()).setSubject(transfer.getSubject()).setPrice(transfer.getPrice())
                    .setUserAccount(transfer.getUserAccount()).setUserName(transfer.getUserName())
                    .setNotifyUrl(channelService.buildNotifyUrl("transfer", channel.getId())));
        } catch (Exception e) {
            log.warn("[invokeTransfer] 转账 {} 调用渠道异常，等待补查", transfer.getNo(), e);
            return;
        }
        if (result.getStatus() == PayClientStatus.WAITING) {
            transferMapper.updateByIdAndStatus(transfer.getId(), List.of(PayTransferStatusEnum.WAITING.getStatus()),
                    new PayTransferDO().setStatus(PayTransferStatusEnum.PROCESSING.getStatus())
                            .setChannelTransferNo(result.getChannelTransferNo()));
            return;
        }
        try {
            notifyTransfer(channel, result);
        } catch (Exception e) {
            // 转账已发起并落库，回写失败不能让发起方误以为转账没有创建；单据保持等待，由补查再处理
            log.warn("[invokeTransfer] 转账 {} 的渠道结果回写失败，等待补查", transfer.getNo(), e);
        }
    }

    /**
     * 回调、同步结果、补查共用。自带事务（经 TransactionTemplate），本类内部调用也生效
     */
    @Override
    public void notifyTransfer(PayChannelDO channel, PayTransferResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            PayTransferDO transfer = transferMapper.selectByNo(result.getOutTransferNo());
            if (transfer == null) {
                throw exception(PAY_TRANSFER_NOT_FOUND);
            }
            // 回调须来自发起该转账的渠道：用渠道 A 的密钥验过签的回调，不能完成渠道 B 上的转账
            if (!Objects.equals(transfer.getChannelId(), channel.getId())) {
                log.error("[notifyTransfer] 转账 {} 属于渠道 {}，却由渠道 {} 通知，拒绝", transfer.getNo(), transfer.getChannelId(), channel.getId());
                throw exception(PAY_TRANSFER_NOT_FOUND);
            }
            if (!UNFINISHED.contains(transfer.getStatus()) || result.getStatus() == PayClientStatus.WAITING) {
                return;
            }
            boolean success = result.getStatus() == PayClientStatus.SUCCESS;
            // 成功才核对金额（失败结果不带有效金额），在任何写入之前拒绝
            if (success && result.getPrice() != null && !result.getPrice().equals(transfer.getPrice())) {
                log.error("[notifyTransfer] 转账 {} 渠道金额 {} 与转账单 {} 不一致，拒绝", transfer.getNo(), result.getPrice(), transfer.getPrice());
                throw exception(PAY_TRANSFER_NOTIFY_PRICE_MISMATCH);
            }
            LocalDateTime successTime = success ? ObjectUtil.defaultIfNull(result.getSuccessTime(), LocalDateTime.now()) : null;
            PayTransferDO update = new PayTransferDO()
                    .setStatus((success ? PayTransferStatusEnum.SUCCESS : PayTransferStatusEnum.FAILURE).getStatus())
                    .setSuccessTime(successTime).setChannelTransferNo(result.getChannelTransferNo())
                    .setChannelErrorCode(result.getErrorCode()).setChannelErrorMsg(StrUtil.maxLength(result.getErrorMsg(), 500))
                    .setChannelNotifyData(StrUtil.maxLength(result.getRawData(), 4000));
            // 条件更新：影响 0 行即已被他人处理，按幂等返回
            if (transferMapper.updateByIdAndStatus(transfer.getId(), UNFINISHED, update) == 0) {
                return;
            }
            PayAppDO app = appService.getApp(transfer.getAppId());
            outboxMessageService.saveAndPublish(success ? PayTransferMessage.TOPIC_SUCCESS : PayTransferMessage.TOPIC_FAILURE,
                    new PayTransferMessage().setTenantId(transfer.getTenantId()).setAppId(app.getId())
                            .setAppKey(app.getAppKey()).setTransferId(transfer.getId()).setTransferNo(transfer.getNo())
                            .setMerchantTransferId(transfer.getMerchantTransferId()).setPrice(transfer.getPrice())
                            .setStatus(update.getStatus()).setSuccessTime(successTime).setChannelErrorMsg(result.getErrorMsg()));
        });
    }

    /**
     * 补查：等待中的转账按同一转账单号重发（渠道按转账单号幂等），进行中的向渠道查询结果。
     * 按编号游标逐页处理：某条长期查询失败时，不会占住队首、饿死后面的转账
     */
    @Override
    public int syncTransfer(int limit) {
        if (limit < 1) {
            return 0;
        }
        LocalDateTime before = LocalDateTime.now().minus(payProperties.getJob().getSyncDelay());
        int count = 0;
        long afterId = 0L;
        List<PayTransferDO> page;
        do {
            long cursor = afterId;
            page = TenantUtils.executeIgnore(() -> transferMapper.selectListByStatusAndCreateTimeLt(
                    UNFINISHED, before, cursor, limit));
            for (PayTransferDO transfer : page) {
                // 先推进游标再处理：失败的转账不会让本轮循环停住或原地打转
                afterId = transfer.getId();
                try {
                    TenantUtils.execute(transfer.getTenantId(), () -> syncOne(transfer));
                    count++;
                } catch (Exception e) {
                    log.warn("[syncTransfer] 转账 {} 补查失败", transfer.getNo(), e);
                }
            }
        } while (page.size() >= limit);
        return count;
    }

    private void syncOne(PayTransferDO transfer) {
        if (PayTransferStatusEnum.WAITING.getStatus().equals(transfer.getStatus())) {
            invokeTransfer(transfer.getId());
            return;
        }
        PayChannelDO channel = channelService.getChannel(transfer.getChannelId());
        if (channel == null) {
            log.error("[syncTransfer] 转账 {} 的渠道 {} 已删除，无法补查", transfer.getNo(), transfer.getChannelId());
            return;
        }
        notifyTransfer(channel, channelService.getPayClient(channel).getTransfer(transfer.getNo()));
    }

    @Override
    public PayTransferDO getTransfer(Long id) {
        return transferMapper.selectById(id);
    }

    @Override
    public PageResult<PayTransferDO> getTransferPage(PayTransferPageReqVO reqVO) {
        return transferMapper.selectPage(reqVO);
    }
}
