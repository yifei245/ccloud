package com.ccloud.module.open.controller.admin.api;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.open.controller.admin.api.vo.OpenApiPageReqVO;
import com.ccloud.module.open.controller.admin.api.vo.OpenApiRespVO;
import com.ccloud.module.open.controller.admin.api.vo.OpenApiSaveReqVO;
import com.ccloud.module.open.controller.admin.api.vo.OpenApiSimpleRespVO;
import com.ccloud.module.open.service.api.OpenApiService;
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

@Tag(name = "管理后台 - 开放接口")
@RestController
@RequestMapping("/admin-api/open/api")
@Validated
public class OpenApiController {

    @Resource
    private OpenApiService openApiService;

    @PostMapping("/create")
    @Operation(summary = "创建开放接口", description = "仅系统租户可操作")
    @PreAuthorize("@ss.hasPermission('open:api:create')")
    public CommonResult<Long> createApi(@Valid @RequestBody OpenApiSaveReqVO createReqVO) {
        return success(openApiService.createApi(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改开放接口", description = "仅系统租户可操作")
    @PreAuthorize("@ss.hasPermission('open:api:update')")
    public CommonResult<Boolean> updateApi(@Valid @RequestBody OpenApiSaveReqVO updateReqVO) {
        openApiService.updateApi(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除开放接口", description = "仅系统租户可操作；同时删除所有应用对该接口的授权")
    @Parameter(name = "id", description = "接口编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('open:api:delete')")
    public CommonResult<Boolean> deleteApi(@RequestParam("id") Long id) {
        openApiService.deleteApi(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得开放接口")
    @Parameter(name = "id", description = "接口编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('open:api:query')")
    public CommonResult<OpenApiRespVO> getApi(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(openApiService.getApi(id), OpenApiRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得开放接口分页")
    @PreAuthorize("@ss.hasPermission('open:api:query')")
    public CommonResult<PageResult<OpenApiRespVO>> getApiPage(@Valid OpenApiPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(openApiService.getApiPage(pageReqVO), OpenApiRespVO.class));
    }

    @GetMapping("/list-all-simple")
    @Operation(summary = "获得开启的开放接口精简列表", description = "供应用授权时勾选")
    @PreAuthorize("@ss.hasPermission('open:api:query')")
    public CommonResult<List<OpenApiSimpleRespVO>> getSimpleApiList() {
        return success(BeanUtils.toBean(openApiService.getEnabledApiList(), OpenApiSimpleRespVO.class));
    }
}
