package com.ccloud.module.system.controller.admin.tenant;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.tenant.vo.packages.TenantPackagePageReqVO;
import com.ccloud.module.system.controller.admin.tenant.vo.packages.TenantPackageRespVO;
import com.ccloud.module.system.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import com.ccloud.module.system.controller.admin.tenant.vo.packages.TenantPackageSimpleRespVO;
import com.ccloud.module.system.service.tenant.TenantPackageService;
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

@Tag(name = "管理后台 - 租户套餐")
@RestController
@RequestMapping("/admin-api/system/tenant-package")
@Validated
public class TenantPackageController {

    @Resource
    private TenantPackageService tenantPackageService;

    @PostMapping("/create")
    @Operation(summary = "创建租户套餐")
    @PreAuthorize("@ss.hasPermission('system:tenant-package:create')")
    public CommonResult<Long> createTenantPackage(@Valid @RequestBody TenantPackageSaveReqVO createReqVO) {
        return success(tenantPackageService.createTenantPackage(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新租户套餐", description = "菜单范围变化时同步到使用该套餐的所有租户")
    @PreAuthorize("@ss.hasPermission('system:tenant-package:update')")
    public CommonResult<Boolean> updateTenantPackage(@Valid @RequestBody TenantPackageSaveReqVO updateReqVO) {
        tenantPackageService.updateTenantPackage(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除租户套餐")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:tenant-package:delete')")
    public CommonResult<Boolean> deleteTenantPackage(@RequestParam("id") Long id) {
        tenantPackageService.deleteTenantPackage(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除租户套餐")
    @Parameter(name = "ids", description = "编号列表", required = true, example = "1024,2048")
    @PreAuthorize("@ss.hasPermission('system:tenant-package:delete')")
    public CommonResult<Boolean> deleteTenantPackageList(@RequestParam("ids") List<Long> ids) {
        tenantPackageService.deleteTenantPackageList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得租户套餐")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:tenant-package:query')")
    public CommonResult<TenantPackageRespVO> getTenantPackage(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(tenantPackageService.getTenantPackage(id), TenantPackageRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得租户套餐分页")
    @PreAuthorize("@ss.hasPermission('system:tenant-package:query')")
    public CommonResult<PageResult<TenantPackageRespVO>> getTenantPackagePage(@Valid TenantPackagePageReqVO pageVO) {
        return success(BeanUtils.toBean(tenantPackageService.getTenantPackagePage(pageVO), TenantPackageRespVO.class));
    }

    /**
     * 租户表单的套餐下拉框使用，只返回开启的套餐
     */
    @GetMapping({"/simple-list", "/get-simple-list"})
    @Operation(summary = "获取租户套餐精简信息列表", description = "只包含被开启的租户套餐，主要用于前端的下拉选项")
    @PreAuthorize("@ss.hasPermission('system:tenant:query')")
    public CommonResult<List<TenantPackageSimpleRespVO>> getTenantPackageList() {
        return success(BeanUtils.toBean(
                tenantPackageService.getTenantPackageListByStatus(CommonStatusEnum.ENABLE.getStatus()),
                TenantPackageSimpleRespVO.class));
    }
}
