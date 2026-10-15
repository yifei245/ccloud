package com.ccloud.module.pay.controller.admin.wallet;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletRechargeRespVO;
import com.ccloud.module.pay.service.wallet.PayWalletRechargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 钱包充值记录")
@RestController
@RequestMapping("/admin-api/pay/wallet-recharge")
@Validated
public class PayWalletRechargeController {

    @Resource
    private PayWalletRechargeService rechargeService;

    @GetMapping("/page")
    @Operation(summary = "获得钱包充值记录分页", description = "不传钱包编号则查全部")
    @Parameter(name = "walletId", description = "钱包编号", example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:wallet-recharge:query')")
    public CommonResult<PageResult<PayWalletRechargeRespVO>> getRechargePage(
            @RequestParam(value = "walletId", required = false) Long walletId, @Valid PageParam pageParam) {
        return success(BeanUtils.toBean(rechargeService.getRechargePage(walletId, pageParam), PayWalletRechargeRespVO.class));
    }

    @PutMapping("/refund")
    @Operation(summary = "充值退款", description = "先扣回到账总额再退款；余额不足时不能退款")
    @Parameter(name = "id", description = "充值编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:wallet-recharge:refund')")
    public CommonResult<Boolean> refundRecharge(@RequestParam("id") Long id, HttpServletRequest request) {
        rechargeService.refundRecharge(id, ServletUtils.getClientIP(request));
        return success(true);
    }
}
