package com.ccloud.module.system.controller.admin.i18n;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.i18n.vo.I18nMessagePageReqVO;
import com.ccloud.module.system.controller.admin.i18n.vo.I18nMessageRespVO;
import com.ccloud.module.system.controller.admin.i18n.vo.I18nMessageSaveReqVO;
import com.ccloud.module.system.service.i18n.I18nMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 国际化文案")
@RestController
@RequestMapping("/admin-api/system/i18n-message")
@Validated
public class I18nMessageController {

    @Resource
    private I18nMessageService i18nMessageService;

    @PostMapping("/create")
    @Operation(summary = "创建国际化文案", description = "保存后各服务自动刷新")
    @PreAuthorize("@ss.hasPermission('system:i18n:create')")
    public CommonResult<Long> createI18nMessage(@Valid @RequestBody I18nMessageSaveReqVO createReqVO) {
        return success(i18nMessageService.createI18nMessage(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改国际化文案", description = "保存后各服务自动刷新")
    @PreAuthorize("@ss.hasPermission('system:i18n:update')")
    public CommonResult<Boolean> updateI18nMessage(@Valid @RequestBody I18nMessageSaveReqVO updateReqVO) {
        i18nMessageService.updateI18nMessage(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除国际化文案", description = "删除后回退到 properties 基线文案")
    @Parameter(name = "id", description = "文案编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:i18n:delete')")
    public CommonResult<Boolean> deleteI18nMessage(@RequestParam("id") Long id) {
        i18nMessageService.deleteI18nMessage(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得国际化文案")
    @Parameter(name = "id", description = "文案编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:i18n:query')")
    public CommonResult<I18nMessageRespVO> getI18nMessage(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(i18nMessageService.getI18nMessage(id), I18nMessageRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得国际化文案分页")
    @PreAuthorize("@ss.hasPermission('system:i18n:query')")
    public CommonResult<PageResult<I18nMessageRespVO>> getI18nMessagePage(@Valid I18nMessagePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(i18nMessageService.getI18nMessagePage(pageReqVO), I18nMessageRespVO.class));
    }
}
