package com.ccloud.module.pay.service.wallet;

import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargePackagePageReqVO;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargePackageSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;
import com.ccloud.module.pay.test.BasePayDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_RECHARGE_PACKAGE_NAME_EXISTS;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.WALLET_RECHARGE_PACKAGE_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

@Import(PayWalletRechargePackageServiceImpl.class)
class PayWalletRechargePackageServiceImplTest extends BasePayDbUnitTest {

    @Resource
    private PayWalletRechargePackageServiceImpl packageService;

    private PayWalletRechargePackageSaveReqVO req(String name, int payPrice, int status) {
        return new PayWalletRechargePackageSaveReqVO().setName(name).setPayPrice(payPrice).setBonusPrice(100).setStatus(status);
    }

    @Test
    void 套餐名重复报错_更新为自己的名字不报错() {
        Long id = packageService.createPackage(req("ut-充 100", 10000, 0));
        assertServiceException(() -> packageService.createPackage(req("ut-充 100", 20000, 0)), WALLET_RECHARGE_PACKAGE_NAME_EXISTS);

        packageService.updatePackage(req("ut-充 100", 12000, 0).setId(id));
        assertThat(packageService.getPackage(id).getPayPrice()).isEqualTo(12000);

        Long other = packageService.createPackage(req("ut-充 200", 20000, 0));
        assertServiceException(() -> packageService.updatePackage(req("ut-充 100", 20000, 0).setId(other)), WALLET_RECHARGE_PACKAGE_NAME_EXISTS);
    }

    @Test
    void 启用套餐列表不含关闭的套餐_按支付金额升序() {
        packageService.createPackage(req("ut-b", 30000, 0));
        packageService.createPackage(req("ut-a", 10000, 0));
        packageService.createPackage(req("ut-closed", 5000, 1));

        List<PayWalletRechargePackageDO> list = packageService.getEnablePackageList();
        assertThat(list).extracting(PayWalletRechargePackageDO::getName).containsExactly("ut-a", "ut-b");
    }

    @Test
    void 更新或删除不存在的套餐报错_分页按名称模糊查询() {
        assertServiceException(() -> packageService.updatePackage(req("ut-x", 100, 0).setId(-1L)), WALLET_RECHARGE_PACKAGE_NOT_FOUND);
        assertServiceException(() -> packageService.deletePackage(-1L), WALLET_RECHARGE_PACKAGE_NOT_FOUND);

        Long id = packageService.createPackage(req("ut-page-hit", 100, 0));
        packageService.createPackage(req("ut-other", 100, 0));
        PayWalletRechargePackagePageReqVO pageReq = new PayWalletRechargePackagePageReqVO();
        pageReq.setName("page-hit");
        assertThat(packageService.getPackagePage(pageReq).getList())
                .extracting(PayWalletRechargePackageDO::getId).containsExactly(id);
        packageService.deletePackage(id);
        assertThat(packageService.getPackage(id)).isNull();
    }
}
