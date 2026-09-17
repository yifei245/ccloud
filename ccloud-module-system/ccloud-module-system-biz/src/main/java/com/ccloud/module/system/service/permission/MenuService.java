package com.ccloud.module.system.service.permission;

import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuListReqVO;
import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuSaveVO;
import com.ccloud.module.system.dal.dataobject.permission.MenuDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * 菜单 Service
 */
public interface MenuService {

    Long createMenu(@Valid MenuSaveVO createReqVO);

    void updateMenu(@Valid MenuSaveVO updateReqVO);

    /**
     * 删除菜单并清理角色关联；存在子菜单时拒绝
     */
    void deleteMenu(Long id);

    void deleteMenuList(List<Long> ids);

    MenuDO getMenu(Long id);

    List<MenuDO> getMenuList();

    List<MenuDO> getMenuList(Collection<Long> ids);

    List<MenuDO> getMenuList(MenuListReqVO reqVO);

    /**
     * 按当前租户套餐过滤的菜单列表；系统租户不过滤
     */
    List<MenuDO> getMenuListByTenant(MenuListReqVO reqVO);

    /**
     * 移除停用的菜单，以及祖先菜单被停用的菜单（父级停用后整棵子树不可用）
     */
    List<MenuDO> filterDisableMenus(List<MenuDO> menus);

    /**
     * @return 权限标识对应的<b>开启</b>菜单编号；菜单停用即视为该权限不可用
     */
    List<Long> getMenuIdListByPermissionFromCache(String permission);
}
