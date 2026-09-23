package com.ccloud.module.system.controller.admin.mail;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplatePageReqVO;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplateRespVO;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplateSaveReqVO;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplateSendReqVO;
import com.ccloud.module.system.service.mail.MailSendService;
import com.ccloud.module.system.service.mail.MailTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 邮件模板")
@RestController
@RequestMapping("/admin-api/system/mail-template")
@Validated
public class MailTemplateController {

    @Resource
    private MailTemplateService mailTemplateService;
    @Resource
    private MailSendService mailSendService;

    @PostMapping("/create")
    @Operation(summary = "创建邮件模板")
    @PreAuthorize("@ss.hasPermission('system:mail-template:create')")
    public CommonResult<Long> createMailTemplate(@Valid @RequestBody MailTemplateSaveReqVO createReqVO) {
        return success(mailTemplateService.createMailTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改邮件模板")
    @PreAuthorize("@ss.hasPermission('system:mail-template:update')")
    public CommonResult<Boolean> updateMailTemplate(@Valid @RequestBody MailTemplateSaveReqVO updateReqVO) {
        mailTemplateService.updateMailTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除邮件模板")
    @Parameter(name = "id", description = "模板编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:mail-template:delete')")
    public CommonResult<Boolean> deleteMailTemplate(@RequestParam("id") Long id) {
        mailTemplateService.deleteMailTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得邮件模板")
    @Parameter(name = "id", description = "模板编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:mail-template:query')")
    public CommonResult<MailTemplateRespVO> getMailTemplate(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(mailTemplateService.getMailTemplate(id), MailTemplateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得邮件模板分页")
    @PreAuthorize("@ss.hasPermission('system:mail-template:query')")
    public CommonResult<PageResult<MailTemplateRespVO>> getMailTemplatePage(@Valid MailTemplatePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(mailTemplateService.getMailTemplatePage(pageReqVO), MailTemplateRespVO.class));
    }

    @PostMapping("/send-mail")
    @Operation(summary = "发送测试邮件", description = "按模板真实发送，返回邮件日志编号")
    @PreAuthorize("@ss.hasPermission('system:mail-template:send-mail')")
    public CommonResult<Long> sendMail(@Valid @RequestBody MailTemplateSendReqVO sendReqVO) {
        return success(mailSendService.sendSingleMailToAdmin(sendReqVO.getMail(), getLoginUserId(),
                sendReqVO.getTemplateCode(), sendReqVO.getTemplateParams()));
    }
}
