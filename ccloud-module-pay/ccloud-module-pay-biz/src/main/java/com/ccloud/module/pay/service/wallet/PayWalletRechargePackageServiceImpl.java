package com.ccloud.module.pay.service.wallet;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargePackagePageReqVO;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargePackageSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;
import com.ccloud.module.pay.dal.mysql.wallet.PayWalletRechargePackageMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_RECHARGE_PACKAGE_NAME_EXISTS;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_RECHARGE_PACKAGE_NOT_FOUND;

@Service
@Validated
public class PayWalletRechargePackageServiceImpl implements PayWalletRechargePackageService {

    @Resource
    private PayWalletRechargePackageMapper packageMapper;

    @Override
    public Long createPackage(PayWalletRechargePackageSaveReqVO reqVO) {
        validatePackageNameUnique(null, reqVO.getName());
        PayWalletRechargePackageDO pkg = new PayWalletRechargePackageDO().setName(reqVO.getName())
                .setPayPrice(reqVO.getPayPrice()).setBonusPrice(reqVO.getBonusPrice()).setStatus(reqVO.getStatus());
        packageMapper.insert(pkg);
        return pkg.getId();
    }

    @Override
    public void updatePackage(PayWalletRechargePackageSaveReqVO reqVO) {
        validatePackageExists(reqVO.getId());
        validatePackageNameUnique(reqVO.getId(), reqVO.getName());
        packageMapper.updateById(new PayWalletRechargePackageDO().setId(reqVO.getId()).setName(reqVO.getName())
                .setPayPrice(reqVO.getPayPrice()).setBonusPrice(reqVO.getBonusPrice()).setStatus(reqVO.getStatus()));
    }

    @Override
    public void deletePackage(Long id) {
        validatePackageExists(id);
        packageMapper.deleteById(id);
    }

    @Override
    public PayWalletRechargePackageDO getPackage(Long id) {
        return packageMapper.selectById(id);
    }

    @Override
    public PageResult<PayWalletRechargePackageDO> getPackagePage(PayWalletRechargePackagePageReqVO reqVO) {
        return packageMapper.selectPage(reqVO);
    }

    @Override
    public List<PayWalletRechargePackageDO> getEnablePackageList() {
        return packageMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private void validatePackageExists(Long id) {
        if (packageMapper.selectById(id) == null) {
            throw exception(WALLET_RECHARGE_PACKAGE_NOT_FOUND);
        }
    }

    private void validatePackageNameUnique(Long id, String name) {
        PayWalletRechargePackageDO existing = packageMapper.selectByName(name);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(WALLET_RECHARGE_PACKAGE_NAME_EXISTS);
        }
    }
}
