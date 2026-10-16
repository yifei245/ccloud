package com.ccloud.module.pay.controller.admin.wallet;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletPageReqVO;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRespVO;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletUpdateBalanceReqVO;
import com.ccloud.module.pay.service.wallet.PayWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 会员钱包")
@RestController
@RequestMapping("/admin-api/pay/wallet")
@Validated
public class PayWalletController {

    @Resource
    private PayWalletService payWalletService;

    @GetMapping("/page")
    @Operation(summary = "获得会员钱包分页")
    @PreAuthorize("@ss.hasPermission('pay:wallet:query')")
    public CommonResult<PageResult<PayWalletRespVO>> getWalletPage(@Valid PayWalletPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(payWalletService.getWalletPage(pageReqVO), PayWalletRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员钱包")
    @Parameter(name = "id", description = "钱包编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:wallet:query')")
    public CommonResult<PayWalletRespVO> getWallet(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(payWalletService.getWallet(id), PayWalletRespVO.class));
    }

    @PutMapping("/update-balance")
    @Operation(summary = "调整会员钱包余额", description = "金额为正数增加、负数减少，减少不能超过余额；写一条后台调整流水")
    @PreAuthorize("@ss.hasPermission('pay:wallet:update-balance')")
    public CommonResult<Boolean> updateWalletBalance(@Valid @RequestBody PayWalletUpdateBalanceReqVO reqVO) {
        payWalletService.adjustBalance(reqVO.getId(), reqVO.getPrice(), reqVO.getReason());
        return success(true);
    }
}
