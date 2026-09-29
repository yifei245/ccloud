package com.ccloud.module.system.controller.admin.sms;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelPageReqVO;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelRespVO;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelSaveReqVO;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelSimpleRespVO;
import com.ccloud.module.system.service.sms.SmsChannelService;
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

@Tag(name = "管理后台 - 短信渠道")
@RestController
@RequestMapping("/admin-api/system/sms-channel")
@Validated
public class SmsChannelController {

    @Resource
    private SmsChannelService smsChannelService;

    @PostMapping("/create")
    @Operation(summary = "创建短信渠道")
    @PreAuthorize("@ss.hasPermission('system:sms-channel:create')")
    public CommonResult<Long> createSmsChannel(@Valid @RequestBody SmsChannelSaveReqVO createReqVO) {
        return success(smsChannelService.createSmsChannel(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改短信渠道")
    @PreAuthorize("@ss.hasPermission('system:sms-channel:update')")
    public CommonResult<Boolean> updateSmsChannel(@Valid @RequestBody SmsChannelSaveReqVO updateReqVO) {
        smsChannelService.updateSmsChannel(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除短信渠道")
    @Parameter(name = "id", description = "渠道编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:sms-channel:delete')")
    public CommonResult<Boolean> deleteSmsChannel(@RequestParam("id") Long id) {
        smsChannelService.deleteSmsChannel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得短信渠道")
    @Parameter(name = "id", description = "渠道编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:sms-channel:query')")
    public CommonResult<SmsChannelRespVO> getSmsChannel(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(smsChannelService.getSmsChannel(id), SmsChannelRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得短信渠道分页")
    @PreAuthorize("@ss.hasPermission('system:sms-channel:query')")
    public CommonResult<PageResult<SmsChannelRespVO>> getSmsChannelPage(@Valid SmsChannelPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(smsChannelService.getSmsChannelPage(pageReqVO), SmsChannelRespVO.class));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获得短信渠道精简列表", description = "包含全部渠道，用于模板管理的渠道下拉框")
    @PreAuthorize("@ss.hasPermission('system:sms-template:query')")
    public CommonResult<List<SmsChannelSimpleRespVO>> getSimpleSmsChannelList() {
        return success(BeanUtils.toBean(smsChannelService.getSmsChannelList(), SmsChannelSimpleRespVO.class));
    }
}
