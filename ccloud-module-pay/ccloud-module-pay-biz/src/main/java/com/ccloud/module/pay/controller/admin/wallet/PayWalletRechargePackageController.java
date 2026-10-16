package com.ccloud.module.pay.controller.admin.wallet;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargePackagePageReqVO;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargePackageRespVO;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargePackageSaveReqVO;
import com.ccloud.module.pay.service.wallet.PayWalletRechargePackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 钱包充值套餐")
@RestController
@RequestMapping("/admin-api/pay/wallet-recharge-package")
@Validated
public class PayWalletRechargePackageController {

    @Resource
    private PayWalletRechargePackageService packageService;

    @PostMapping("/create")
    @Operation(summary = "创建钱包充值套餐")
    @PreAuthorize("@ss.hasPermission('pay:wallet-recharge-package:create')")
    public CommonResult<Long> createPackage(@Valid @RequestBody PayWalletRechargePackageSaveReqVO reqVO) {
        return success(packageService.createPackage(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改钱包充值套餐")
    @PreAuthorize("@ss.hasPermission('pay:wallet-recharge-package:update')")
    public CommonResult<Boolean> updatePackage(@Valid @RequestBody PayWalletRechargePackageSaveReqVO reqVO) {
        packageService.updatePackage(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除钱包充值套餐")
    @Parameter(name = "id", description = "套餐编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:wallet-recharge-package:delete')")
    public CommonResult<Boolean> deletePackage(@RequestParam("id") Long id) {
        packageService.deletePackage(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得钱包充值套餐")
    @Parameter(name = "id", description = "套餐编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:wallet-recharge-package:query')")
    public CommonResult<PayWalletRechargePackageRespVO> getPackage(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(packageService.getPackage(id), PayWalletRechargePackageRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得钱包充值套餐分页")
    @PreAuthorize("@ss.hasPermission('pay:wallet-recharge-package:query')")
    public CommonResult<PageResult<PayWalletRechargePackageRespVO>> getPackagePage(@Valid PayWalletRechargePackagePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(packageService.getPackagePage(pageReqVO), PayWalletRechargePackageRespVO.class));
    }
}
