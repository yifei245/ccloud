package com.ccloud.module.pay.controller.app.wallet;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.pay.controller.app.wallet.vo.AppPayWalletRechargeCreateReqVO;
import com.ccloud.module.pay.controller.app.wallet.vo.AppPayWalletRechargeCreateRespVO;
import com.ccloud.module.pay.controller.app.wallet.vo.AppPayWalletRechargeRespVO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import com.ccloud.module.pay.service.wallet.PayWalletRechargeService;
import com.ccloud.module.pay.service.wallet.PayWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 钱包充值")
@RestController
@RequestMapping("/app-api/pay/wallet-recharge")
@Validated
public class AppPayWalletRechargeController {

    @Resource
    private PayWalletRechargeService rechargeService;
    @Resource
    private PayWalletService walletService;

    @PostMapping("/create")
    @Operation(summary = "创建钱包充值", description = "返回支付单编号，随后用它提交支付；充值不能使用钱包余额支付")
    public CommonResult<AppPayWalletRechargeCreateRespVO> createRecharge(
            @Valid @RequestBody AppPayWalletRechargeCreateReqVO reqVO, HttpServletRequest request) {
        PayWalletRechargeDO recharge = rechargeService.createRecharge(SecurityFrameworkUtils.getLoginUserId(),
                UserTypeEnum.MEMBER.getValue(), reqVO, ServletUtils.getClientIP(request));
        return success(new AppPayWalletRechargeCreateRespVO().setId(recharge.getId()).setPayOrderId(recharge.getPayOrderId()));
    }

    @GetMapping("/page")
    @Operation(summary = "获得我的钱包充值记录分页", description = "按时间倒序")
    public CommonResult<PageResult<AppPayWalletRechargeRespVO>> getRechargePage(@Valid PageParam pageParam) {
        PayWalletDO wallet = walletService.getOrCreateWallet(
                SecurityFrameworkUtils.getLoginUserId(), UserTypeEnum.MEMBER.getValue());
        return success(BeanUtils.toBean(rechargeService.getRechargePage(wallet.getId(), pageParam),
                AppPayWalletRechargeRespVO.class));
    }
}
