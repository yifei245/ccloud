package com.ccloud.module.open.controller.admin.app;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.open.controller.admin.app.vo.*;
import com.ccloud.module.open.dal.dataobject.app.OpenAppDO;
import com.ccloud.module.open.service.app.OpenAppSecretUtils;
import com.ccloud.module.open.service.app.OpenAppService;
import com.ccloud.module.open.service.notify.OpenNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 开放应用")
@RestController
@RequestMapping("/admin-api/open/app")
@Validated
public class OpenAppController {

    @Resource
    private OpenAppService openAppService;
    @Resource
    private OpenNotifyService openNotifyService;

    @PostMapping("/create")
    @Operation(summary = "创建开放应用", description = "应用密钥只在响应中返回一次")
    @PreAuthorize("@ss.hasPermission('open:app:create')")
    public CommonResult<OpenAppCreateRespVO> createApp(@Valid @RequestBody OpenAppSaveReqVO createReqVO) {
        return success(openAppService.createApp(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改开放应用", description = "不能修改应用标识与密钥")
    @PreAuthorize("@ss.hasPermission('open:app:update')")
    public CommonResult<Boolean> updateApp(@Valid @RequestBody OpenAppSaveReqVO updateReqVO) {
        openAppService.updateApp(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除开放应用", description = "同时删除它的接口授权")
    @Parameter(name = "id", description = "应用编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('open:app:delete')")
    public CommonResult<Boolean> deleteApp(@RequestParam("id") Long id) {
        openAppService.deleteApp(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得开放应用", description = "密钥已掩码")
    @Parameter(name = "id", description = "应用编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('open:app:query')")
    public CommonResult<OpenAppRespVO> getApp(@RequestParam("id") Long id) {
        OpenAppDO app = openAppService.getApp(id);
        return success(app == null ? null : toResp(app));
    }

    @GetMapping("/page")
    @Operation(summary = "获得开放应用分页", description = "密钥已掩码")
    @PreAuthorize("@ss.hasPermission('open:app:query')")
    public CommonResult<PageResult<OpenAppRespVO>> getAppPage(@Valid OpenAppPageReqVO pageReqVO) {
        PageResult<OpenAppDO> page = openAppService.getAppPage(pageReqVO);
        return success(new PageResult<>(page.getList().stream().map(OpenAppController::toResp).toList(), page.getTotal()));
    }

    @PutMapping("/reset-secret")
    @Operation(summary = "重置应用密钥", description = "旧密钥立即失效；新密钥只在响应中返回一次")
    @Parameter(name = "id", description = "应用编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('open:app:reset-secret')")
    public CommonResult<String> resetAppSecret(@RequestParam("id") Long id) {
        return success(openAppService.resetAppSecret(id));
    }

    @PutMapping("/assign-api")
    @Operation(summary = "授权应用可调用的接口", description = "整体替换；只能选择开启的接口")
    @PreAuthorize("@ss.hasPermission('open:app:assign-api')")
    public CommonResult<Boolean> assignApi(@Valid @RequestBody OpenAppAssignApiReqVO reqVO) {
        openAppService.assignApis(reqVO.getAppId(), reqVO.getApiIds());
        return success(true);
    }

    @GetMapping("/get-api-ids")
    @Operation(summary = "获得应用已授权的接口编号")
    @Parameter(name = "id", description = "应用编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('open:app:query')")
    public CommonResult<Set<Long>> getAppApiIds(@RequestParam("id") Long id) {
        return success(openAppService.getAppApiIds(id));
    }

    @PostMapping("/test-notify")
    @Operation(summary = "发送测试回调", description = "事件 open.test，返回回调日志编号；可在回调日志中查看结果")
    @Parameter(name = "id", description = "应用编号", required = true)
    @PreAuthorize("@ss.hasPermission('open:app:test-notify')")
    public CommonResult<Long> testNotify(@RequestParam("id") Long id) {
        return success(openNotifyService.sendTestNotify(id));
    }

    private static OpenAppRespVO toResp(OpenAppDO app) {
        return BeanUtils.toBean(app, OpenAppRespVO.class).setAppSecret(OpenAppSecretUtils.mask(app.getAppSecret()));
    }
}
