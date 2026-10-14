package com.ccloud.module.pay.framework.pay.core.client.impl.wallet;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import com.ccloud.module.pay.enums.PayChannelEnum;
import com.ccloud.module.pay.enums.wallet.PayWalletBizTypeEnum;
import com.ccloud.module.pay.framework.pay.core.client.PayClient;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayDisplayMode;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayNotifyRequest;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderUnifiedReq;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundUnifiedReq;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferUnifiedReq;
import com.ccloud.module.pay.service.wallet.PayWalletService;

import java.time.LocalDateTime;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.CHANNEL_OPERATION_NOT_SUPPORTED;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_BALANCE_NOT_ENOUGH;

/**
 * 钱包渠道：站内余额支付。没有回调，查询按流水判断；扣款流水的业务编号即支付尝试的外部单号
 */
public class WalletPayClient implements PayClient {

    private final Long channelId;
    private final PayWalletService walletService;

    public WalletPayClient(Long channelId, PayWalletService walletService) {
        this.channelId = channelId;
        this.walletService = walletService;
    }

    @Override
    public Long getChannelId() {
        return channelId;
    }

    @Override
    public PayOrderResult unifiedOrder(PayOrderUnifiedReq req) {
        if (req.getUserId() == null) {
            return PayOrderResult.failure(req.getOutTradeNo(), "USER_REQUIRED", "钱包支付须指定付款会员", null);
        }
        PayWalletDO wallet = walletService.getOrCreateWallet(req.getUserId(), req.getUserType());
        try {
            PayWalletTransactionDO tx = walletService.reduceBalance(wallet.getId(), PayWalletBizTypeEnum.PAYMENT,
                    req.getOutTradeNo(), "支付：" + req.getSubject(), req.getPrice());
            return PayOrderResult.success(req.getOutTradeNo(), tx.getNo(), req.getPrice(), LocalDateTime.now(), null);
        } catch (ServiceException e) {
            if (!WALLET_BALANCE_NOT_ENOUGH.getCode().equals(e.getCode())) {
                throw e;
            }
            return PayOrderResult.failure(req.getOutTradeNo(), String.valueOf(e.getCode()), e.getMessage(), null);
        }
    }

    @Override
    public PayOrderResult getOrder(String outTradeNo) {
        PayWalletTransactionDO tx = walletService.getTransaction(PayWalletBizTypeEnum.PAYMENT, outTradeNo);
        return tx == null ? PayOrderResult.waiting(outTradeNo, PayDisplayMode.NONE, null)
                : PayOrderResult.success(outTradeNo, tx.getNo(), -tx.getPrice(), tx.getCreateTime(), null);
    }

    @Override
    public PayRefundResult unifiedRefund(PayRefundUnifiedReq req) {
        PayWalletTransactionDO payment = walletService.getTransaction(PayWalletBizTypeEnum.PAYMENT, req.getOutTradeNo());
        if (payment == null) {
            return PayRefundResult.failure(req.getOutRefundNo(), "PAYMENT_NOT_FOUND", "未找到钱包支付流水", null);
        }
        PayWalletTransactionDO tx = walletService.addBalance(payment.getWalletId(), PayWalletBizTypeEnum.PAYMENT_REFUND,
                req.getOutRefundNo(), "退款：" + StrUtil.nullToEmpty(req.getReason()), req.getRefundPrice());
        return PayRefundResult.success(req.getOutRefundNo(), tx.getNo(), req.getRefundPrice(), LocalDateTime.now(), null);
    }

    /** 钱包退款是同步的：没有流水说明发起时未入账（如进程中断），返回等待，由补查任务重发（按退款单号幂等） */
    @Override
    public PayRefundResult getRefund(String outTradeNo, String outRefundNo) {
        PayWalletTransactionDO tx = walletService.getTransaction(PayWalletBizTypeEnum.PAYMENT_REFUND, outRefundNo);
        return tx == null ? PayRefundResult.waiting(outRefundNo)
                : PayRefundResult.success(outRefundNo, tx.getNo(), tx.getPrice(), tx.getCreateTime(), null);
    }

    @Override
    public PayTransferResult unifiedTransfer(PayTransferUnifiedReq req) {
        Long userId = NumberUtil.isLong(req.getUserAccount()) ? Long.valueOf(req.getUserAccount()) : null;
        if (userId == null) {
            return PayTransferResult.failure(req.getOutTransferNo(), "ACCOUNT_INVALID", "收款账号不是会员编号", null);
        }
        PayWalletDO wallet = walletService.getOrCreateWallet(userId, UserTypeEnum.MEMBER.getValue());
        PayWalletTransactionDO tx = walletService.addBalance(wallet.getId(), PayWalletBizTypeEnum.TRANSFER_IN,
                req.getOutTransferNo(), "转账：" + StrUtil.nullToEmpty(req.getSubject()), req.getPrice());
        return PayTransferResult.success(req.getOutTransferNo(), tx.getNo(), req.getPrice(), LocalDateTime.now(), null);
    }

    /** 没有流水返回等待，由补查任务重发（按转账单号幂等） */
    @Override
    public PayTransferResult getTransfer(String outTransferNo) {
        PayWalletTransactionDO tx = walletService.getTransaction(PayWalletBizTypeEnum.TRANSFER_IN, outTransferNo);
        return tx == null ? PayTransferResult.waiting(outTransferNo)
                : PayTransferResult.success(outTransferNo, tx.getNo(), tx.getPrice(), tx.getCreateTime(), null);
    }

    @Override
    public PayOrderResult parseOrderNotify(PayNotifyRequest request) {
        throw exception(CHANNEL_OPERATION_NOT_SUPPORTED, PayChannelEnum.WALLET.getCode());
    }

    @Override
    public PayRefundResult parseRefundNotify(PayNotifyRequest request) {
        throw exception(CHANNEL_OPERATION_NOT_SUPPORTED, PayChannelEnum.WALLET.getCode());
    }

    @Override
    public PayTransferResult parseTransferNotify(PayNotifyRequest request) {
        throw exception(CHANNEL_OPERATION_NOT_SUPPORTED, PayChannelEnum.WALLET.getCode());
    }
}
