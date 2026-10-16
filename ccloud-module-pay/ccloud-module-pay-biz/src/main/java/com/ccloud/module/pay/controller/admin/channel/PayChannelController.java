package com.ccloud.module.pay.controller.admin.channel;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.controller.admin.channel.vo.PayChannelRespVO;
import com.ccloud.module.pay.controller.admin.channel.vo.PayChannelSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.service.channel.PayChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 支付渠道")
// 渠道配置是 JSON 字符串，内部的密钥名脱敏器看不到：整段屏蔽 config。访问日志与异常日志共用这套规则
@ApiAccessLog(sanitizeKeys = "config")
@RestController
@RequestMapping("/admin-api/pay/channel")
@Validated
public class PayChannelController {

    @Resource
    private PayChannelService payChannelService;
    @Resource
    private PayClientFactory payClientFactory;

    @PostMapping("/create")
    @Operation(summary = "创建支付渠道")
    @PreAuthorize("@ss.hasPermission('pay:channel:create')")
    public CommonResult<Long> createChannel(@Valid @RequestBody PayChannelSaveReqVO createReqVO) {
        return success(payChannelService.createChannel(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改支付渠道", description = "不能修改所属应用与渠道编码；配置中的敏感字段提交 *** 表示保持原值")
    @PreAuthorize("@ss.hasPermission('pay:channel:update')")
    public CommonResult<Boolean> updateChannel(@Valid @RequestBody PayChannelSaveReqVO updateReqVO) {
        payChannelService.updateChannel(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除支付渠道")
    @Parameter(name = "id", description = "渠道编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:channel:delete')")
    public CommonResult<Boolean> deleteChannel(@RequestParam("id") Long id) {
        payChannelService.deleteChannel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得支付渠道", description = "配置中的敏感字段已脱敏")
    @Parameter(name = "id", description = "渠道编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:channel:query')")
    public CommonResult<PayChannelRespVO> getChannel(@RequestParam("id") Long id) {
        PayChannelDO channel = payChannelService.getChannel(id);
        return success(channel == null ? null : toResp(channel));
    }

    @GetMapping("/list")
    @Operation(summary = "获得应用的支付渠道列表", description = "配置中的敏感字段已脱敏")
    @Parameter(name = "appId", description = "应用编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('pay:channel:query')")
    public CommonResult<List<PayChannelRespVO>> getChannelList(@RequestParam("appId") Long appId) {
        return success(payChannelService.getChannelListByAppId(appId).stream().map(this::toResp).toList());
    }

    /**
     * 回显前脱敏。编码已不受支持（如生产环境里遗留的 mock 渠道）时无从得知哪些字段敏感，返回 {} 而不抛错
     */
    private PayChannelRespVO toResp(PayChannelDO channel) {
        String config = payClientFactory.isSupported(channel.getCode())
                ? payClientFactory.maskConfig(channel.getCode(), channel.getConfig()) : "{}";
        return BeanUtils.toBean(channel, PayChannelRespVO.class).setConfig(config);
    }
}
