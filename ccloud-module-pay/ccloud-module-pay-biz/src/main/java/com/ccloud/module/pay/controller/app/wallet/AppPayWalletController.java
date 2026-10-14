package com.ccloud.module.pay.controller.app.wallet;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.pay.controller.app.wallet.vo.AppPayWalletRespVO;
import com.ccloud.module.pay.service.wallet.PayWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 钱包")
@RestController
@RequestMapping("/app-api/pay/wallet")
@Validated
public class AppPayWalletController {

    @Resource
    private PayWalletService payWalletService;

    @GetMapping("/get")
    @Operation(summary = "获得我的钱包", description = "首次访问自动开通")
    public CommonResult<AppPayWalletRespVO> getWallet() {
        return success(BeanUtils.toBean(payWalletService.getOrCreateWallet(
                SecurityFrameworkUtils.getLoginUserId(), UserTypeEnum.MEMBER.getValue()), AppPayWalletRespVO.class));
    }
}
