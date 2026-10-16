package com.ccloud.module.pay.service.wallet;

import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletPageReqVO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import com.ccloud.module.pay.enums.wallet.PayWalletBizTypeEnum;

/**
 * 钱包服务：余额变更一律原子更新并写流水，同一业务单据对同一钱包只入账一次
 */
public interface PayWalletService {

    /**
     * 获得用户的钱包，不存在则开通
     */
    PayWalletDO getOrCreateWallet(Long userId, Integer userType);

    PayWalletDO getWallet(Long id);

    PageResult<PayWalletDO> getWalletPage(PayWalletPageReqVO reqVO);

    PageResult<PayWalletTransactionDO> getWalletTransactionPage(Long walletId, PageParam pageParam);

    /**
     * 按业务类型与业务编号查询流水，不存在返回 null
     */
    PayWalletTransactionDO getTransaction(PayWalletBizTypeEnum bizType, String bizId);

    /**
     * 入账。同一 (钱包, 业务类型, 业务编号) 已有流水时直接返回该流水
     */
    PayWalletTransactionDO addBalance(Long walletId, PayWalletBizTypeEnum bizType, String bizId, String title, int price);

    /**
     * 扣减。余额不足抛 WALLET_BALANCE_NOT_ENOUGH；同一 (钱包, 业务类型, 业务编号) 已有流水时直接返回该流水
     */
    PayWalletTransactionDO reduceBalance(Long walletId, PayWalletBizTypeEnum bizType, String bizId, String title, int price);

    /**
     * 后台调整余额，price 可正可负，0 抛 WALLET_ADJUST_PRICE_ZERO
     */
    void adjustBalance(Long walletId, int price, String reason);

}
