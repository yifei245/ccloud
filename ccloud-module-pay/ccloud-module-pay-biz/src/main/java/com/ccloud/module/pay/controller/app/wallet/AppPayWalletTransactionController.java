package com.ccloud.module.pay.controller.app.wallet;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.pay.controller.app.wallet.vo.AppPayWalletTransactionRespVO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.ccloud.module.pay.service.wallet.PayWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 钱包流水")
@RestController
@RequestMapping("/app-api/pay/wallet-transaction")
@Validated
public class AppPayWalletTransactionController {

    @Resource
    private PayWalletService payWalletService;

    @GetMapping("/page")
    @Operation(summary = "获得我的钱包流水分页", description = "按时间倒序")
    public CommonResult<PageResult<AppPayWalletTransactionRespVO>> getWalletTransactionPage(@Valid PageParam pageParam) {
        PayWalletDO wallet = payWalletService.getOrCreateWallet(
                SecurityFrameworkUtils.getLoginUserId(), UserTypeEnum.MEMBER.getValue());
        return success(BeanUtils.toBean(payWalletService.getWalletTransactionPage(wallet.getId(), pageParam),
                AppPayWalletTransactionRespVO.class));
    }
}
