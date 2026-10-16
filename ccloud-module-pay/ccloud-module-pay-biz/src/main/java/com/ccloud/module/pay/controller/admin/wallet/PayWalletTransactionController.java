package com.ccloud.module.pay.controller.admin.wallet;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.controller.admin.wallet.vo.PayWalletTransactionRespVO;
import com.ccloud.module.pay.service.wallet.PayWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 钱包流水")
@RestController
@RequestMapping("/admin-api/pay/wallet-transaction")
@Validated
public class PayWalletTransactionController {

    @Resource
    private PayWalletService payWalletService;

    @GetMapping("/page")
    @Operation(summary = "获得钱包流水分页", description = "按钱包查询，按时间倒序")
    @Parameter(name = "walletId", description = "钱包编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:wallet:query')")
    public CommonResult<PageResult<PayWalletTransactionRespVO>> getWalletTransactionPage(
            @RequestParam("walletId") @NotNull(message = "钱包编号不能为空") Long walletId, @Valid PageParam pageParam) {
        return success(BeanUtils.toBean(payWalletService.getWalletTransactionPage(walletId, pageParam),
                PayWalletTransactionRespVO.class));
    }
}
