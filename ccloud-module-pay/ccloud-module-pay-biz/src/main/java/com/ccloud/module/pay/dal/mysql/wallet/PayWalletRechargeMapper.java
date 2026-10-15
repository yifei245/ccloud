package com.ccloud.module.pay.dal.mysql.wallet;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayWalletRechargeMapper extends BaseMapperX<PayWalletRechargeDO> {

    default PageResult<PayWalletRechargeDO> selectPage(Long walletId, PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<PayWalletRechargeDO>()
                .eqIfPresent(PayWalletRechargeDO::getWalletId, walletId).orderByDesc(PayWalletRechargeDO::getId));
    }

    /**
     * 当前读并加行锁（SELECT ... FOR UPDATE）。并发的充值退款借此串行：后到者等待先到者提交后，读到的是最新的退款单编号
     */
    default PayWalletRechargeDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<PayWalletRechargeDO>().eq(PayWalletRechargeDO::getId, id).last("FOR UPDATE"));
    }

    /** 未支付 → 已支付，防止重复入账 */
    default int updateWhenPaid(Long id, PayWalletRechargeDO update) {
        return update(update, new LambdaUpdateWrapper<PayWalletRechargeDO>().eq(PayWalletRechargeDO::getId, id)
                .eq(PayWalletRechargeDO::getPayStatus, false));
    }

    /** 已支付且未发起过退款 → 退款中（写入退款单编号） */
    default int updateWhenRefunding(Long id, PayWalletRechargeDO update) {
        return update(update, new LambdaUpdateWrapper<PayWalletRechargeDO>().eq(PayWalletRechargeDO::getId, id)
                .eq(PayWalletRechargeDO::getPayStatus, true).isNull(PayWalletRechargeDO::getPayRefundId));
    }

    /** 退款中 → 退款成功或失败 */
    default int updateWhenRefundFinished(Long id, PayWalletRechargeDO update) {
        return update(update, new LambdaUpdateWrapper<PayWalletRechargeDO>().eq(PayWalletRechargeDO::getId, id)
                .isNotNull(PayWalletRechargeDO::getPayRefundId)
                .eq(PayWalletRechargeDO::getRefundStatus, PayRefundStatusEnum.WAITING.getStatus()));
    }
}
