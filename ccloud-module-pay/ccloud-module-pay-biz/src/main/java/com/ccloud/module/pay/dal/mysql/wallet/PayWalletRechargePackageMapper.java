package com.ccloud.module.pay.dal.mysql.wallet;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargePackagePageReqVO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PayWalletRechargePackageMapper extends BaseMapperX<PayWalletRechargePackageDO> {

    default PayWalletRechargePackageDO selectByName(String name) {
        return selectOne(PayWalletRechargePackageDO::getName, name);
    }

    default PageResult<PayWalletRechargePackageDO> selectPage(PayWalletRechargePackagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PayWalletRechargePackageDO>()
                .likeIfPresent(PayWalletRechargePackageDO::getName, reqVO.getName())
                .eqIfPresent(PayWalletRechargePackageDO::getStatus, reqVO.getStatus())
                .orderByDesc(PayWalletRechargePackageDO::getId));
    }

    default List<PayWalletRechargePackageDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<PayWalletRechargePackageDO>()
                .eq(PayWalletRechargePackageDO::getStatus, status)
                .orderByAsc(PayWalletRechargePackageDO::getPayPrice));
    }
}
