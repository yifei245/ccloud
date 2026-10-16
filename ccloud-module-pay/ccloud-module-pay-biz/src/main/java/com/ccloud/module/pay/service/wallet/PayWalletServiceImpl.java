package com.ccloud.module.pay.service.wallet;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletPageReqVO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import com.ccloud.module.pay.dal.mysql.wallet.PayWalletMapper;
import com.ccloud.module.pay.dal.mysql.wallet.PayWalletTransactionMapper;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.wallet.PayWalletBizTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_ADJUST_PRICE_ZERO;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_BALANCE_NOT_ENOUGH;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_NOT_FOUND;

@Service
@Validated
@Slf4j
public class PayWalletServiceImpl implements PayWalletService {

    @Resource
    private PayWalletMapper walletMapper;
    @Resource
    private PayWalletTransactionMapper transactionMapper;
    @Resource
    private PayNoRedisDAO payNoRedisDAO;

    @Override
    public PayWalletDO getOrCreateWallet(Long userId, Integer userType) {
        PayWalletDO wallet = walletMapper.selectByUser(userId, userType);
        if (wallet != null) {
            return wallet;
        }
        PayWalletDO created = new PayWalletDO().setUserId(userId).setUserType(userType)
                .setBalance(0).setTotalExpense(0).setTotalRecharge(0).setFreezePrice(0);
        try {
            walletMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            // 并发首次访问：另一请求已开通。用当前读重新查询：调用方若处于可重复读事务内，快照读看不到对方刚提交的行
            PayWalletDO existing = walletMapper.selectByUserForShare(userId, userType);
            if (existing == null) {
                throw e;
            }
            return existing;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayWalletTransactionDO addBalance(Long walletId, PayWalletBizTypeEnum bizType, String bizId, String title, int price) {
        Assert.isTrue(price > 0, "入账金额须为正数");
        PayWalletTransactionDO existing = transactionMapper.selectByWalletAndBiz(walletId, bizType.getType(), bizId);
        if (existing != null) {
            return existing;
        }
        int expenseDelta = bizType == PayWalletBizTypeEnum.PAYMENT_REFUND ? -price : 0;
        int rechargeDelta = bizType == PayWalletBizTypeEnum.RECHARGE ? price : 0;
        if (walletMapper.updateBalance(walletId, price, expenseDelta, rechargeDelta) == 0) {
            throw exception(WALLET_NOT_FOUND);
        }
        return insertTransaction(walletId, bizType, bizId, title, price);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayWalletTransactionDO reduceBalance(Long walletId, PayWalletBizTypeEnum bizType, String bizId, String title, int price) {
        Assert.isTrue(price > 0, "扣减金额须为正数");
        PayWalletTransactionDO existing = transactionMapper.selectByWalletAndBiz(walletId, bizType.getType(), bizId);
        if (existing != null) {
            return existing;
        }
        int expenseDelta = bizType == PayWalletBizTypeEnum.PAYMENT ? price : 0;
        int rechargeDelta = bizType == PayWalletBizTypeEnum.RECHARGE_REFUND ? -price : 0;
        if (walletMapper.updateBalance(walletId, -price, expenseDelta, rechargeDelta) == 0) {
            throw exception(walletMapper.selectById(walletId) == null ? WALLET_NOT_FOUND : WALLET_BALANCE_NOT_ENOUGH);
        }
        return insertTransaction(walletId, bizType, bizId, title, -price);
    }

    /**
     * 同一 (钱包, 业务类型, 业务编号) 并发写入时唯一约束冲突：异常抛出使本事务连同余额变更一起回滚，
     * 调用方重试时会命中已有流水直接返回
     */
    private PayWalletTransactionDO insertTransaction(Long walletId, PayWalletBizTypeEnum bizType, String bizId,
                                                     String title, int price) {
        PayWalletTransactionDO transaction = new PayWalletTransactionDO()
                .setNo(payNoRedisDAO.generate(PayNoRedisDAO.WALLET_TRANSACTION_PREFIX)).setWalletId(walletId)
                .setBizType(bizType.getType()).setBizId(bizId).setTitle(StrUtil.maxLength(title, 120)).setPrice(price)
                .setBalance(walletMapper.selectById(walletId).getBalance());
        transactionMapper.insert(transaction);
        return transaction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustBalance(Long walletId, int price, String reason) {
        if (price == 0) {
            throw exception(WALLET_ADJUST_PRICE_ZERO);
        }
        if (walletMapper.selectById(walletId) == null) {
            throw exception(WALLET_NOT_FOUND);
        }
        String bizId = payNoRedisDAO.generate(PayNoRedisDAO.WALLET_ADJUST_PREFIX);
        String title = "后台调整：" + reason;
        if (price > 0) {
            addBalance(walletId, PayWalletBizTypeEnum.ADJUST, bizId, title, price);
        } else {
            reduceBalance(walletId, PayWalletBizTypeEnum.ADJUST, bizId, title, -price);
        }
    }

    @Override
    public PayWalletDO getWallet(Long id) {
        return walletMapper.selectById(id);
    }

    @Override
    public PageResult<PayWalletDO> getWalletPage(PayWalletPageReqVO reqVO) {
        return walletMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<PayWalletTransactionDO> getWalletTransactionPage(Long walletId, PageParam pageParam) {
        return transactionMapper.selectPage(walletId, pageParam);
    }

    @Override
    public PayWalletTransactionDO getTransaction(PayWalletBizTypeEnum bizType, String bizId) {
        return transactionMapper.selectByBiz(bizType.getType(), bizId);
    }
}
