package com.ccloud.module.pay.dal.mysql.wallet;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletPageReqVO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PayWalletMapper extends BaseMapperX<PayWalletDO> {

    default PayWalletDO selectByUser(Long userId, Integer userType) {
        return selectOne(PayWalletDO::getUserId, userId, PayWalletDO::getUserType, userType);
    }

    /**
     * 当前读（共享锁）。开通钱包遇到唯一约束冲突后用它重新查询：快照读在可重复读事务内看不到对方刚提交的行
     */
    default PayWalletDO selectByUserForShare(Long userId, Integer userType) {
        return selectOne(new LambdaQueryWrapperX<PayWalletDO>().eq(PayWalletDO::getUserId, userId)
                .eq(PayWalletDO::getUserType, userType).last("FOR SHARE"));
    }

    default PageResult<PayWalletDO> selectPage(PayWalletPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PayWalletDO>()
                .eqIfPresent(PayWalletDO::getUserId, reqVO.getUserId())
                .eqIfPresent(PayWalletDO::getUserType, reqVO.getUserType())
                .orderByDesc(PayWalletDO::getId));
    }

    /**
     * 原子变更余额与累计值。余额变更后不得为负：条件不满足时影响 0 行（余额不足或钱包不存在）。
     * 禁止先查后改——并发扣款会扣成负数
     */
    @Update("UPDATE pay_wallet SET balance = balance + #{balanceDelta}, total_expense = total_expense + #{expenseDelta}, "
            + "total_recharge = total_recharge + #{rechargeDelta} "
            + "WHERE id = #{id} AND deleted = 0 AND balance + #{balanceDelta} >= 0")
    int updateBalance(@Param("id") Long id, @Param("balanceDelta") int balanceDelta,
                      @Param("expenseDelta") int expenseDelta, @Param("rechargeDelta") int rechargeDelta);
}
