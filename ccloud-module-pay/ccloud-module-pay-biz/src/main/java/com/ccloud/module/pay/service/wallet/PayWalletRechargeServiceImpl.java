package com.ccloud.module.pay.service.wallet;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.number.MoneyUtils;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.ccloud.module.pay.controller.app.wallet.vo.AppPayWalletRechargeCreateReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;
import com.ccloud.module.pay.dal.mysql.wallet.PayWalletRechargeMapper;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.enums.wallet.PayWalletBizTypeEnum;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.mq.message.PayOrderMessage;
import com.ccloud.module.pay.mq.message.PayRefundMessage;
import com.ccloud.module.pay.service.app.PayAppService;
import com.ccloud.module.pay.service.order.PayOrderService;
import com.ccloud.module.pay.service.refund.PayRefundService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class PayWalletRechargeServiceImpl implements PayWalletRechargeService {

    private static final String MERCHANT_ORDER_PREFIX = "WR";
    private static final String MERCHANT_REFUND_SUFFIX = "-R";

    @Resource
    private PayWalletRechargeMapper rechargeMapper;
    @Resource
    private PayWalletRechargePackageService packageService;
    @Resource
    private PayWalletService walletService;
    @Resource
    private PayOrderService orderService;
    @Resource
    private PayRefundService refundService;
    @Resource
    private PayAppService appService;
    @Resource
    private PayProperties payProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayWalletRechargeDO createRecharge(Long userId, Integer userType, AppPayWalletRechargeCreateReqVO reqVO, String userIp) {
        PayAppDO app = getRechargeApp();
        int payPrice;
        int bonusPrice;
        if (reqVO.getPackageId() != null) {
            PayWalletRechargePackageDO pkg = packageService.getPackage(reqVO.getPackageId());
            if (pkg == null || !CommonStatusEnum.ENABLE.getStatus().equals(pkg.getStatus())) {
                throw exception(WALLET_RECHARGE_PACKAGE_NOT_FOUND);
            }
            payPrice = pkg.getPayPrice();
            bonusPrice = pkg.getBonusPrice();
        } else if (reqVO.getPayPrice() != null && reqVO.getPayPrice() > 0) {
            payPrice = reqVO.getPayPrice();
            bonusPrice = 0;
        } else {
            throw exception(WALLET_RECHARGE_PRICE_INVALID);
        }
        PayWalletDO wallet = walletService.getOrCreateWallet(userId, userType);
        PayWalletRechargeDO recharge = new PayWalletRechargeDO().setWalletId(wallet.getId()).setPackageId(reqVO.getPackageId())
                .setPayPrice(payPrice).setBonusPrice(bonusPrice).setTotalPrice(payPrice + bonusPrice).setPayStatus(false)
                .setRefundStatus(PayRefundStatusEnum.WAITING.getStatus())
                .setRefundTotalPrice(0).setRefundPayPrice(0).setRefundBonusPrice(0);
        rechargeMapper.insert(recharge);
        Long orderId = orderService.createOrder(app, new PayOrderCreateReqDTO().setAppKey(app.getAppKey())
                .setMerchantOrderId(MERCHANT_ORDER_PREFIX + recharge.getId()).setSubject("钱包充值")
                .setBody("充值 " + MoneyUtils.fenToYuanStr(payPrice) + " 元").setPrice(payPrice)
                .setExpireTime(LocalDateTime.now().plus(payProperties.getWallet().getRechargeExpire()))
                .setUserIp(userIp).setUserId(userId).setUserType(userType));
        rechargeMapper.updateById(new PayWalletRechargeDO().setId(recharge.getId()).setPayOrderId(orderId));
        return recharge.setPayOrderId(orderId);
    }

    private PayAppDO getRechargeApp() {
        try {
            return appService.validPayApp(payProperties.getWallet().getRechargeAppKey());
        } catch (ServiceException e) {
            throw exception(WALLET_RECHARGE_NOT_OPENED);
        }
    }

    @Override
    public boolean isRechargeApp(Long appId) {
        PayAppDO app = appService.getApp(appId);
        return app != null && app.getAppKey().equals(payProperties.getWallet().getRechargeAppKey());
    }

    @Override
    public PageResult<PayWalletRechargeDO> getRechargePage(Long walletId, PageParam pageParam) {
        return rechargeMapper.selectPage(walletId, pageParam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRechargePaid(PayOrderMessage message) {
        Long rechargeId = parseId(message.getMerchantOrderId(), MERCHANT_ORDER_PREFIX, "");
        if (rechargeId == null) {
            log.warn("[handleRechargePaid] 商户订单号 {} 不是充值单号，忽略", message.getMerchantOrderId());
            return;
        }
        PayWalletRechargeDO recharge = rechargeMapper.selectById(rechargeId);
        if (recharge == null || Boolean.TRUE.equals(recharge.getPayStatus())) {
            return;
        }
        // 消息中的支付单须就是创建充值时关联的那一笔，否则其他应用恰好同名的商户订单号会被误当作充值入账
        if (recharge.getPayOrderId() != null && !recharge.getPayOrderId().equals(message.getOrderId())) {
            log.error("[handleRechargePaid] 充值 {} 关联支付单 {}，消息的支付单 {} 不一致，不入账",
                    recharge.getId(), recharge.getPayOrderId(), message.getOrderId());
            return;
        }
        if (!recharge.getPayPrice().equals(message.getPrice())) {
            log.error("[handleRechargePaid] 充值 {} 实付 {} 与支付金额 {} 不一致，不入账", recharge.getId(), recharge.getPayPrice(), message.getPrice());
            return;
        }
        if (rechargeMapper.updateWhenPaid(recharge.getId(), new PayWalletRechargeDO().setPayStatus(true)
                .setPayOrderId(message.getOrderId()).setPayChannelCode(message.getChannelCode())
                .setPayTime(message.getSuccessTime())) == 0) {
            return;
        }
        walletService.addBalance(recharge.getWalletId(), PayWalletBizTypeEnum.RECHARGE, String.valueOf(recharge.getId()),
                "充值", recharge.getTotalPrice());
    }

    /**
     * 先扣回到账总额（实付加赠送），再对原支付单退实付金额。退款渠道调用由退款服务推迟到本事务提交之后；
     * 退款失败时由 handleRechargeRefunded 把扣回的金额退回。
     * 先对充值记录加行锁再校验状态：并发的退款在此串行，后到者读到先到者写入的退款单编号而被拒绝，不会重复扣回
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundRecharge(Long id, String userIp) {
        PayWalletRechargeDO recharge = rechargeMapper.selectByIdForUpdate(id);
        if (recharge == null) {
            throw exception(WALLET_RECHARGE_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(recharge.getPayStatus()) || recharge.getPayRefundId() != null) {
            throw exception(WALLET_RECHARGE_REFUND_STATUS_ERROR);
        }
        walletService.reduceBalance(recharge.getWalletId(), PayWalletBizTypeEnum.RECHARGE_REFUND, String.valueOf(id),
                "充值退款", recharge.getTotalPrice());
        PayAppDO app = getRechargeApp();
        Long refundId = refundService.createRefund(app, new PayRefundCreateReqDTO().setAppKey(app.getAppKey())
                .setMerchantOrderId(MERCHANT_ORDER_PREFIX + id).setMerchantRefundId(MERCHANT_ORDER_PREFIX + id + MERCHANT_REFUND_SUFFIX)
                .setPrice(recharge.getPayPrice()).setReason("钱包充值退款").setUserIp(StrUtil.blankToDefault(userIp, "127.0.0.1")));
        if (rechargeMapper.updateWhenRefunding(id, new PayWalletRechargeDO().setPayRefundId(refundId)
                .setRefundTotalPrice(recharge.getTotalPrice()).setRefundPayPrice(recharge.getPayPrice())
                .setRefundBonusPrice(recharge.getBonusPrice())) == 0) {
            throw exception(WALLET_RECHARGE_REFUND_STATUS_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRechargeRefunded(PayRefundMessage message) {
        Long rechargeId = parseId(message.getMerchantRefundId(), MERCHANT_ORDER_PREFIX, MERCHANT_REFUND_SUFFIX);
        if (rechargeId == null) {
            log.warn("[handleRechargeRefunded] 商户退款单号 {} 不是充值退款单号，忽略", message.getMerchantRefundId());
            return;
        }
        PayWalletRechargeDO recharge = rechargeMapper.selectById(rechargeId);
        if (recharge == null) {
            return;
        }
        if (recharge.getPayRefundId() != null && !recharge.getPayRefundId().equals(message.getRefundId())) {
            log.error("[handleRechargeRefunded] 充值 {} 关联退款单 {}，消息的退款单 {} 不一致，忽略",
                    recharge.getId(), recharge.getPayRefundId(), message.getRefundId());
            return;
        }
        boolean success = PayRefundStatusEnum.SUCCESS.getStatus().equals(message.getStatus());
        int updated = rechargeMapper.updateWhenRefundFinished(recharge.getId(), new PayWalletRechargeDO()
                .setRefundStatus(message.getStatus()).setRefundTime(success ? message.getSuccessTime() : null));
        if (updated == 0 || success) {
            return;
        }
        // 退款失败：退回发起时扣回的金额。记为充值类型，使累计充值随之恢复
        walletService.addBalance(recharge.getWalletId(), PayWalletBizTypeEnum.RECHARGE,
                recharge.getId() + "-refund-rollback", "充值退款失败退回", recharge.getRefundTotalPrice());
    }

    /**
     * 从 prefix + 编号 + suffix 形式的单号解析充值编号；格式不符或编号不是正整数返回 null
     */
    private static Long parseId(String merchantId, String prefix, String suffix) {
        if (merchantId == null || !merchantId.startsWith(prefix) || !merchantId.endsWith(suffix)
                || merchantId.length() <= prefix.length() + suffix.length()) {
            return null;
        }
        String digits = merchantId.substring(prefix.length(), merchantId.length() - suffix.length());
        if (!digits.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return null;
        }
        try {
            long id = Long.parseLong(digits);
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
