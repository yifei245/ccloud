package com.ccloud.module.system.controller.admin.user;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.excel.core.util.ExcelUtils;
import com.ccloud.module.system.controller.admin.user.vo.user.*;
import com.ccloud.module.system.dal.dataobject.dept.DeptDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.service.dept.DeptService;
import com.ccloud.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.ccloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.ccloud.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 用户")
@RestController
@RequestMapping("/admin-api/system/user")
@Validated
public class UserController {

    @Resource
    private AdminUserService userService;
    @Resource
    private DeptService deptService;

    @PostMapping("/create")
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Long> createUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        return success(userService.createUser(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改用户", description = "不修改密码，重置密码请使用 update-password")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        userService.updateUser(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUser(@RequestParam("id") Long id) {
        userService.deleteUser(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除用户")
    @Parameter(name = "ids", description = "编号列表", required = true, example = "1024,2048")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUserList(@RequestParam("ids") List<Long> ids) {
        userService.deleteUserList(ids);
        return success(true);
    }

    @PutMapping("/update-password")
    @Operation(summary = "重置用户密码")
    @PreAuthorize("@ss.hasPermission('system:user:update-password')")
    public CommonResult<Boolean> updateUserPassword(@Valid @RequestBody UserUpdatePasswordReqVO reqVO) {
        userService.resetUserPassword(reqVO.getId(), reqVO.getPassword());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改用户状态")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUserStatus(@Valid @RequestBody UserUpdateStatusReqVO reqVO) {
        userService.updateUserStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得用户分页列表", description = "受部门数据权限约束")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<PageResult<UserRespVO>> getUserPage(@Valid UserPageReqVO pageReqVO) {
        PageResult<AdminUserDO> pageResult = userService.getUserPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(new PageResult<>(pageResult.getTotal()));
        }
        return success(new PageResult<>(buildUserList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取用户精简信息列表", description = "只包含开启的用户，主要用于前端的下拉选项")
    public CommonResult<List<UserSimpleRespVO>> getSimpleUserList() {
        List<AdminUserDO> list = userService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus());
        Map<Long, DeptDO> deptMap = getDeptMap(list);
        return success(CollectionUtils.convertList(list, user -> BeanUtils.toBean(user, UserSimpleRespVO.class,
                vo -> vo.setDeptName(deptName(deptMap, user.getDeptId())))));
    }

    @GetMapping("/get")
    @Operation(summary = "获得用户详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
        AdminUserDO user = userService.getUser(id);
        if (user == null) {
            return success(null);
        }
        return success(buildUserList(List.of(user)).get(0));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出用户")
    @PreAuthorize("@ss.hasPermission('system:user:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUserList(@Valid UserPageReqVO exportReqVO, HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AdminUserDO> list = userService.getUserPage(exportReqVO).getList();
        ExcelUtils.write(response, "用户数据.xls", "数据", UserRespVO.class, buildUserList(list));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入用户模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<UserImportExcelVO> list = List.of(
                new UserImportExcelVO().setUsername("demo01").setNickname("示例一").setDeptId(100L)
                        .setEmail("demo01@ccloud.local").setMobile("15601691300").setSex(1)
                        .setStatus(CommonStatusEnum.ENABLE.getStatus()),
                new UserImportExcelVO().setUsername("demo02").setNickname("示例二").setDeptId(101L)
                        .setEmail("demo02@ccloud.local").setMobile("15601701300").setSex(2)
                        .setStatus(CommonStatusEnum.DISABLE.getStatus()));
        ExcelUtils.write(response, "用户导入模板.xls", "用户列表", UserImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入用户")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "账号已存在时是否更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('system:user:import')")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<UserImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "updateSupport", required = false,
                                                              defaultValue = "false") Boolean updateSupport)
            throws IOException {
        List<UserImportExcelVO> list = ExcelUtils.read(file, UserImportExcelVO.class);
        return success(userService.importUserList(list, updateSupport));
    }

    private List<UserRespVO> buildUserList(List<AdminUserDO> users) {
        Map<Long, DeptDO> deptMap = getDeptMap(users);
        return CollectionUtils.convertList(users, user -> BeanUtils.toBean(user, UserRespVO.class,
                vo -> vo.setDeptName(deptName(deptMap, user.getDeptId()))));
    }

    private Map<Long, DeptDO> getDeptMap(Collection<AdminUserDO> users) {
        return CollectionUtils.convertMap(
                deptService.getDeptList(CollectionUtils.convertSet(
                        CollectionUtils.filterList(users, user -> user.getDeptId() != null), AdminUserDO::getDeptId)),
                DeptDO::getId);
    }

    private static String deptName(Map<Long, DeptDO> deptMap, Long deptId) {
        DeptDO dept = deptId != null ? deptMap.get(deptId) : null;
        return dept != null ? dept.getName() : null;
    }
}
