package com.ccloud.module.system.controller.admin.mail;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountPageReqVO;
import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountRespVO;
import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountSaveReqVO;
import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountSimpleRespVO;
import com.ccloud.module.system.service.mail.MailAccountService;
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

@Tag(name = "管理后台 - 邮箱账号")
@RestController
@RequestMapping("/admin-api/system/mail-account")
@Validated
public class MailAccountController {

    @Resource
    private MailAccountService mailAccountService;

    @PostMapping("/create")
    @Operation(summary = "创建邮箱账号")
    @PreAuthorize("@ss.hasPermission('system:mail-account:create')")
    public CommonResult<Long> createMailAccount(@Valid @RequestBody MailAccountSaveReqVO createReqVO) {
        return success(mailAccountService.createMailAccount(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改邮箱账号", description = "密码留空表示不修改")
    @PreAuthorize("@ss.hasPermission('system:mail-account:update')")
    public CommonResult<Boolean> updateMailAccount(@Valid @RequestBody MailAccountSaveReqVO updateReqVO) {
        mailAccountService.updateMailAccount(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除邮箱账号")
    @Parameter(name = "id", description = "账号编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:mail-account:delete')")
    public CommonResult<Boolean> deleteMailAccount(@RequestParam("id") Long id) {
        mailAccountService.deleteMailAccount(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得邮箱账号", description = "不返回密码")
    @Parameter(name = "id", description = "账号编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:mail-account:query')")
    public CommonResult<MailAccountRespVO> getMailAccount(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(mailAccountService.getMailAccount(id), MailAccountRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得邮箱账号分页")
    @PreAuthorize("@ss.hasPermission('system:mail-account:query')")
    public CommonResult<PageResult<MailAccountRespVO>> getMailAccountPage(@Valid MailAccountPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(mailAccountService.getMailAccountPage(pageReqVO), MailAccountRespVO.class));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获得邮箱账号精简列表", description = "用于模板管理的账号下拉框")
    @PreAuthorize("@ss.hasPermission('system:mail-template:query')")
    public CommonResult<List<MailAccountSimpleRespVO>> getSimpleMailAccountList() {
        return success(BeanUtils.toBean(mailAccountService.getMailAccountList(), MailAccountSimpleRespVO.class));
    }
}
