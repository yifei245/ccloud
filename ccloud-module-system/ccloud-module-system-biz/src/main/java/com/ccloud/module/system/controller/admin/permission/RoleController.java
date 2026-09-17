package com.ccloud.module.system.controller.admin.permission;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.excel.core.util.ExcelUtils;
import com.ccloud.module.system.controller.admin.permission.vo.role.*;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.service.permission.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.ccloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 角色")
@RestController
@RequestMapping("/admin-api/system/role")
@Validated
public class RoleController {

    @Resource
    private RoleService roleService;

    @PostMapping("/create")
    @Operation(summary = "创建角色")
    @PreAuthorize("@ss.hasPermission('system:role:create')")
    public CommonResult<Long> createRole(@Valid @RequestBody RoleSaveReqVO createReqVO) {
        return success(roleService.createRole(createReqVO, null));
    }

    @PutMapping("/update")
    @Operation(summary = "修改角色", description = "内置角色不允许修改")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> updateRole(@Valid @RequestBody RoleSaveReqVO updateReqVO) {
        roleService.updateRole(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改角色状态")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> updateRoleStatus(@Valid @RequestBody RoleUpdateStatusReqVO reqVO) {
        roleService.updateRoleStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除角色", description = "同时清理用户与菜单关联；内置角色不允许删除")
    @Parameter(name = "id", description = "角色编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> deleteRole(@RequestParam("id") Long id) {
        roleService.deleteRole(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除角色")
    @Parameter(name = "ids", description = "角色编号列表", required = true, example = "1024,2048")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> deleteRoleList(@RequestParam("ids") List<Long> ids) {
        roleService.deleteRoleList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得角色信息")
    @Parameter(name = "id", description = "角色编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<RoleRespVO> getRole(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(roleService.getRole(id), RoleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得角色分页")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<PageResult<RoleRespVO>> getRolePage(@Valid RolePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(roleService.getRolePage(pageReqVO), RoleRespVO.class));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取角色精简信息列表", description = "只包含开启的角色，主要用于前端的下拉选项")
    public CommonResult<List<RoleSimpleRespVO>> getSimpleRoleList() {
        List<RoleDO> list = roleService.getRoleListByStatus(Set.of(CommonStatusEnum.ENABLE.getStatus()));
        list.sort(Comparator.comparing(RoleDO::getSort));
        return success(BeanUtils.toBean(list, RoleSimpleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出角色 Excel")
    @ApiAccessLog(operateType = EXPORT)
    @PreAuthorize("@ss.hasPermission('system:role:export')")
    public void export(HttpServletResponse response, @Valid RolePageReqVO exportReqVO) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RoleDO> list = roleService.getRolePage(exportReqVO).getList();
        ExcelUtils.write(response, "角色数据.xls", "数据", RoleRespVO.class, BeanUtils.toBean(list, RoleRespVO.class));
    }
}
