package com.ccloud.module.system.controller.admin.mail;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.mail.vo.log.MailLogPageReqVO;
import com.ccloud.module.system.controller.admin.mail.vo.log.MailLogRespVO;
import com.ccloud.module.system.service.mail.MailLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 邮件日志")
@RestController
@RequestMapping("/admin-api/system/mail-log")
@Validated
public class MailLogController {

    @Resource
    private MailLogService mailLogService;

    @GetMapping("/page")
    @Operation(summary = "获得邮件日志分页")
    @PreAuthorize("@ss.hasPermission('system:mail-log:query')")
    public CommonResult<PageResult<MailLogRespVO>> getMailLogPage(@Valid MailLogPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(mailLogService.getMailLogPage(pageReqVO), MailLogRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得邮件日志")
    @Parameter(name = "id", description = "日志编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:mail-log:query')")
    public CommonResult<MailLogRespVO> getMailLog(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(mailLogService.getMailLog(id), MailLogRespVO.class));
    }
}
