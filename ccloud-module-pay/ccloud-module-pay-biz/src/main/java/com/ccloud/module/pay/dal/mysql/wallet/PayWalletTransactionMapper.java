package com.ccloud.module.pay.dal.mysql.wallet;

import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayWalletTransactionMapper extends BaseMapperX<PayWalletTransactionDO> {

    default PayWalletTransactionDO selectByWalletAndBiz(Long walletId, Integer bizType, String bizId) {
        return selectOne(new LambdaQueryWrapperX<PayWalletTransactionDO>().eq(PayWalletTransactionDO::getWalletId, walletId)
                .eq(PayWalletTransactionDO::getBizType, bizType).eq(PayWalletTransactionDO::getBizId, bizId));
    }

    default PayWalletTransactionDO selectByBiz(Integer bizType, String bizId) {
        return selectOne(PayWalletTransactionDO::getBizType, bizType, PayWalletTransactionDO::getBizId, bizId);
    }

    default PageResult<PayWalletTransactionDO> selectPage(Long walletId, PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<PayWalletTransactionDO>()
                .eq(PayWalletTransactionDO::getWalletId, walletId).orderByDesc(PayWalletTransactionDO::getId));
    }
}
