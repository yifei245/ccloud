package com.ccloud.module.system.controller.admin.notify;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.notify.vo.template.NotifyTemplatePageReqVO;
import com.ccloud.module.system.controller.admin.notify.vo.template.NotifyTemplateRespVO;
import com.ccloud.module.system.controller.admin.notify.vo.template.NotifyTemplateSaveReqVO;
import com.ccloud.module.system.controller.admin.notify.vo.template.NotifyTemplateSendReqVO;
import com.ccloud.module.system.service.notify.NotifySendService;
import com.ccloud.module.system.service.notify.NotifyTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 站内信模板")
@RestController
@RequestMapping("/admin-api/system/notify-template")
@Validated
public class NotifyTemplateController {

    @Resource
    private NotifyTemplateService notifyTemplateService;
    @Resource
    private NotifySendService notifySendService;

    @PostMapping("/create")
    @Operation(summary = "创建站内信模板")
    @PreAuthorize("@ss.hasPermission('system:notify-template:create')")
    public CommonResult<Long> createNotifyTemplate(@Valid @RequestBody NotifyTemplateSaveReqVO createReqVO) {
        return success(notifyTemplateService.createNotifyTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改站内信模板")
    @PreAuthorize("@ss.hasPermission('system:notify-template:update')")
    public CommonResult<Boolean> updateNotifyTemplate(@Valid @RequestBody NotifyTemplateSaveReqVO updateReqVO) {
        notifyTemplateService.updateNotifyTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除站内信模板")
    @Parameter(name = "id", description = "模板编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notify-template:delete')")
    public CommonResult<Boolean> deleteNotifyTemplate(@RequestParam("id") Long id) {
        notifyTemplateService.deleteNotifyTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得站内信模板")
    @Parameter(name = "id", description = "模板编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notify-template:query')")
    public CommonResult<NotifyTemplateRespVO> getNotifyTemplate(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(notifyTemplateService.getNotifyTemplate(id), NotifyTemplateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得站内信模板分页")
    @PreAuthorize("@ss.hasPermission('system:notify-template:query')")
    public CommonResult<PageResult<NotifyTemplateRespVO>> getNotifyTemplatePage(@Valid NotifyTemplatePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(notifyTemplateService.getNotifyTemplatePage(pageReqVO), NotifyTemplateRespVO.class));
    }

    @PostMapping("/send-notify")
    @Operation(summary = "发送测试站内信", description = "模板关闭时不发送，返回 null")
    @PreAuthorize("@ss.hasPermission('system:notify-template:send-notify')")
    public CommonResult<Long> sendNotify(@Valid @RequestBody NotifyTemplateSendReqVO sendReqVO) {
        return success(notifySendService.sendSingleNotify(sendReqVO.getUserId(), sendReqVO.getUserType(),
                sendReqVO.getTemplateCode(), sendReqVO.getTemplateParams()));
    }
}
