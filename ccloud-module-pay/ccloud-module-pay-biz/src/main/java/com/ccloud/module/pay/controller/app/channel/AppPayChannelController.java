package com.ccloud.module.pay.controller.app.channel;

import com.ccloud.module.pay.controller.app.channel.vo.AppPayChannelRespVO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.enums.PayChannelEnum;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.service.channel.PayChannelService;
import com.ccloud.module.pay.service.wallet.PayWalletRechargeService;
import com.ccloud.framework.common.pojo.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 支付渠道")
@RestController
@RequestMapping("/app-api/pay/channel")
@Validated
public class AppPayChannelController {

    @Resource
    private PayChannelService channelService;
    @Resource
    private PayWalletRechargeService rechargeService;
    @Resource
    private PayClientFactory payClientFactory;

    @GetMapping("/list")
    @Operation(summary = "获得应用下可用的支付渠道列表", description = "只含已开启且本环境有实现的渠道；钱包充值应用不含钱包渠道（充值不能用钱包余额支付）")
    @Parameter(name = "appId", description = "支付应用编号", required = true, example = "1")
    public CommonResult<List<AppPayChannelRespVO>> getChannelList(@RequestParam("appId") Long appId) {
        boolean rechargeApp = rechargeService.isRechargeApp(appId);
        return success(channelService.getEnableChannelList(appId).stream()
                // 编码已不受支持（如关闭模拟支付后遗留的 mock 渠道）的渠道发起不了支付，不给用户选
                .filter(channel -> payClientFactory.isSupported(channel.getCode()))
                .filter(channel -> !(rechargeApp && PayChannelEnum.WALLET.getCode().equals(channel.getCode())))
                .map(AppPayChannelController::toResp)
                .toList());
    }

    private static AppPayChannelRespVO toResp(PayChannelDO channel) {
        PayChannelEnum channelEnum = PayChannelEnum.getByCode(channel.getCode());
        return new AppPayChannelRespVO().setCode(channel.getCode())
                .setName(channelEnum == null ? channel.getCode() : channelEnum.getName());
    }
}
