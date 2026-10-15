package com.ccloud.module.pay.controller.app.wallet;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.controller.app.wallet.vo.AppPayWalletRechargePackageRespVO;
import com.ccloud.module.pay.service.wallet.PayWalletRechargePackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 钱包充值套餐")
@RestController
@RequestMapping("/app-api/pay/wallet-recharge-package")
@Validated
public class AppPayWalletRechargePackageController {

    @Resource
    private PayWalletRechargePackageService packageService;

    @GetMapping("/list")
    @Operation(summary = "获得开启的钱包充值套餐列表", description = "按支付金额升序")
    public CommonResult<List<AppPayWalletRechargePackageRespVO>> getPackageList() {
        return success(BeanUtils.toBean(packageService.getEnablePackageList(), AppPayWalletRechargePackageRespVO.class));
    }
}
