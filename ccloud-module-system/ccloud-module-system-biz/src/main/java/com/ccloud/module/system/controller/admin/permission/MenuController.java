package com.ccloud.module.system.controller.admin.permission;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuListReqVO;
import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuRespVO;
import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuSaveVO;
import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuSimpleRespVO;
import com.ccloud.module.system.dal.dataobject.permission.MenuDO;
import com.ccloud.module.system.service.permission.MenuService;
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

@Tag(name = "管理后台 - 菜单")
@RestController
@RequestMapping("/admin-api/system/menu")
@Validated
public class MenuController {

    @Resource
    private MenuService menuService;

    @PostMapping("/create")
    @Operation(summary = "创建菜单")
    @PreAuthorize("@ss.hasPermission('system:menu:create')")
    public CommonResult<Long> createMenu(@Valid @RequestBody MenuSaveVO createReqVO) {
        return success(menuService.createMenu(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改菜单")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    public CommonResult<Boolean> updateMenu(@Valid @RequestBody MenuSaveVO updateReqVO) {
        menuService.updateMenu(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除菜单", description = "存在子菜单时不允许删除")
    @Parameter(name = "id", description = "菜单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:menu:delete')")
    public CommonResult<Boolean> deleteMenu(@RequestParam("id") Long id) {
        menuService.deleteMenu(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除菜单", description = "可同时选中父子菜单")
    @Parameter(name = "ids", description = "菜单编号列表", required = true, example = "1024,2048")
    @PreAuthorize("@ss.hasPermission('system:menu:delete')")
    public CommonResult<Boolean> deleteMenuList(@RequestParam("ids") List<Long> ids) {
        menuService.deleteMenuList(ids);
        return success(true);
    }

    /**
     * 菜单维护页使用：不按租户过滤，只有系统租户的管理员才有菜单管理权限
     */
    @GetMapping("/list")
    @Operation(summary = "获取菜单列表", description = "用于菜单管理界面")
    @PreAuthorize("@ss.hasPermission('system:menu:query')")
    public CommonResult<List<MenuRespVO>> getMenuList(MenuListReqVO reqVO) {
        return success(BeanUtils.toBean(menuService.getMenuList(reqVO), MenuRespVO.class));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取菜单精简信息列表", description = "只包含开启且在当前租户套餐内的菜单，用于角色分配菜单、租户套餐选择菜单")
    public CommonResult<List<MenuSimpleRespVO>> getSimpleMenuList() {
        MenuListReqVO reqVO = new MenuListReqVO();
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        List<MenuDO> list = menuService.filterDisableMenus(menuService.getMenuListByTenant(reqVO));
        return success(BeanUtils.toBean(list, MenuSimpleRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取菜单信息")
    @Parameter(name = "id", description = "菜单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:menu:query')")
    public CommonResult<MenuRespVO> getMenu(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(menuService.getMenu(id), MenuRespVO.class));
    }
}
