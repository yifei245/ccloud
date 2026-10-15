package com.ccloud.module.pay.service.wallet;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargePackagePageReqVO;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargePackageSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;

import java.util.List;

/**
 * 钱包充值套餐服务
 */
public interface PayWalletRechargePackageService {

    Long createPackage(PayWalletRechargePackageSaveReqVO reqVO);

    void updatePackage(PayWalletRechargePackageSaveReqVO reqVO);

    void deletePackage(Long id);

    PayWalletRechargePackageDO getPackage(Long id);

    PageResult<PayWalletRechargePackageDO> getPackagePage(PayWalletRechargePackagePageReqVO reqVO);

    /**
     * 获得开启的套餐，按支付金额升序
     */
    List<PayWalletRechargePackageDO> getEnablePackageList();
}
