package com.ccloud.module.system.service.permission;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.security.core.service.PermissionCacheRefresher;
import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuListReqVO;
import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuSaveVO;
import com.ccloud.module.system.dal.dataobject.permission.MenuDO;
import com.ccloud.module.system.dal.mysql.permission.MenuMapper;
import com.ccloud.module.system.enums.permission.MenuTypeEnum;
import com.ccloud.module.system.service.tenant.TenantMenuHandler;
import com.ccloud.module.system.service.tenant.TenantService;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@Import({MenuServiceImpl.class, CacheTestConfiguration.class})
class MenuServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private MenuService menuService;
    @Resource
    private MenuMapper menuMapper;

    @MockitoBean
    private PermissionService permissionService;
    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private PermissionCacheRefresher permissionCacheRefresher;

    // ========== 创建 ==========

    @Test
    void createMenu_成功() {
        MenuDO dir = insert("系统管理", 0L, MenuTypeEnum.DIR, null, CommonStatusEnum.ENABLE);
        Long id = menuService.createMenu(newSaveVO("用户管理", dir.getId(), MenuTypeEnum.MENU, null));

        MenuDO menu = menuMapper.selectById(id);
        assertThat(menu.getName()).isEqualTo("用户管理");
        assertThat(menu.getParentId()).isEqualTo(dir.getId());
        assertThat(menu.getPath()).isEqualTo("/path");
    }

    @Test
    void createMenu_按钮不保留路由与组件属性() {
        MenuDO page = insert("用户管理", 0L, MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);
        Long id = menuService.createMenu(newSaveVO("新增", page.getId(), MenuTypeEnum.BUTTON, "system:user:create"));

        MenuDO button = menuMapper.selectById(id);
        assertThat(button.getPermission()).isEqualTo("system:user:create");
        assertThat(button.getPath()).isEmpty();
        assertThat(button.getComponent()).isNullOrEmpty();
        assertThat(button.getIcon()).isEmpty();
    }

    @Test
    void createMenu_父菜单为按钮时失败() {
        MenuDO button = insert("新增", 0L, MenuTypeEnum.BUTTON, "a:b:c", CommonStatusEnum.ENABLE);
        assertServiceException(() -> menuService.createMenu(newSaveVO("子", button.getId(), MenuTypeEnum.BUTTON, "x")),
                MENU_PARENT_NOT_DIR_OR_MENU);
    }

    @Test
    void createMenu_父菜单不存在() {
        assertServiceException(() -> menuService.createMenu(newSaveVO("子", -1L, MenuTypeEnum.MENU, null)),
                MENU_PARENT_NOT_EXISTS);
    }

    @Test
    void createMenu_同级重名失败_不同级允许同名() {
        MenuDO a = insert("A", 0L, MenuTypeEnum.DIR, null, CommonStatusEnum.ENABLE);
        MenuDO b = insert("B", 0L, MenuTypeEnum.DIR, null, CommonStatusEnum.ENABLE);
        menuService.createMenu(newSaveVO("列表", a.getId(), MenuTypeEnum.MENU, null));

        assertServiceException(() -> menuService.createMenu(newSaveVO("列表", a.getId(), MenuTypeEnum.MENU, null)),
                MENU_NAME_DUPLICATE);
        assertThat(menuService.createMenu(newSaveVO("列表", b.getId(), MenuTypeEnum.MENU, null))).isNotNull();
    }

    @Test
    void 非系统租户不能维护菜单() {
        MenuDO menu = insert("共用菜单", 0L, MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);
        // 非系统租户时 handleTenantMenu 会回调
        doAnswer(inv -> {
            inv.<TenantMenuHandler>getArgument(0).handle(Set.of(menu.getId()));
            return null;
        }).when(tenantService).handleTenantMenu(any());

        assertServiceException(() -> menuService.createMenu(newSaveVO("新菜单", 0L, MenuTypeEnum.MENU, null)),
                MENU_ONLY_SYSTEM_TENANT);
        MenuSaveVO update = newSaveVO("改名", 0L, MenuTypeEnum.MENU, null);
        update.setId(menu.getId());
        assertServiceException(() -> menuService.updateMenu(update), MENU_ONLY_SYSTEM_TENANT);
        assertServiceException(() -> menuService.deleteMenu(menu.getId()), MENU_ONLY_SYSTEM_TENANT);
        assertThat(menuMapper.selectById(menu.getId()).getName()).isEqualTo("共用菜单");
    }

    // ========== 修改 ==========

    @Test
    void updateMenu_成功_清除权限缓存并广播() {
        MenuDO button = insert("导出", 0L, MenuTypeEnum.BUTTON, "old:perm", CommonStatusEnum.ENABLE);
        assertThat(menuService.getMenuIdListByPermissionFromCache("old:perm")).containsExactly(button.getId());

        MenuSaveVO reqVO = newSaveVO("导出", 0L, MenuTypeEnum.BUTTON, "new:perm");
        reqVO.setId(button.getId());
        menuService.updateMenu(reqVO);

        assertThat(menuService.getMenuIdListByPermissionFromCache("old:perm")).isEmpty();
        assertThat(menuService.getMenuIdListByPermissionFromCache("new:perm")).containsExactly(button.getId());
        verify(permissionCacheRefresher).publish(null);
    }

    @Test
    void updateMenu_停用后权限不再可用() {
        MenuDO button = insert("删除", 0L, MenuTypeEnum.BUTTON, "p:delete", CommonStatusEnum.ENABLE);
        assertThat(menuService.getMenuIdListByPermissionFromCache("p:delete")).hasSize(1);

        MenuSaveVO reqVO = newSaveVO("删除", 0L, MenuTypeEnum.BUTTON, "p:delete");
        reqVO.setId(button.getId());
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        menuService.updateMenu(reqVO);

        assertThat(menuService.getMenuIdListByPermissionFromCache("p:delete")).isEmpty();
    }

    @Test
    void updateMenu_不存在() {
        MenuSaveVO reqVO = newSaveVO("x", 0L, MenuTypeEnum.MENU, null);
        reqVO.setId(-1L);
        assertServiceException(() -> menuService.updateMenu(reqVO), MENU_NOT_EXISTS);
    }

    @Test
    void updateMenu_不能设置自己或自己的子孙为父菜单() {
        MenuDO dir = insert("目录", 0L, MenuTypeEnum.DIR, null, CommonStatusEnum.ENABLE);
        MenuDO child = insert("子目录", dir.getId(), MenuTypeEnum.DIR, null, CommonStatusEnum.ENABLE);
        MenuDO grandChild = insert("页面", child.getId(), MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);

        MenuSaveVO self = newSaveVO("目录", dir.getId(), MenuTypeEnum.DIR, null);
        self.setId(dir.getId());
        assertServiceException(() -> menuService.updateMenu(self), MENU_PARENT_ERROR);

        MenuSaveVO cycle = newSaveVO("目录", grandChild.getId(), MenuTypeEnum.DIR, null);
        cycle.setId(dir.getId());
        assertServiceException(() -> menuService.updateMenu(cycle), MENU_PARENT_ERROR);
    }

    // ========== 删除 ==========

    @Test
    void deleteMenu_成功并清理角色关联() {
        MenuDO menu = insert("待删", 0L, MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);
        menuService.deleteMenu(menu.getId());
        assertThat(menuMapper.selectById(menu.getId())).isNull();
        verify(permissionService).processMenuDeleted(menu.getId());
    }

    @Test
    void deleteMenu_有子菜单时失败() {
        MenuDO dir = insert("目录", 0L, MenuTypeEnum.DIR, null, CommonStatusEnum.ENABLE);
        insert("子", dir.getId(), MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);
        assertServiceException(() -> menuService.deleteMenu(dir.getId()), MENU_EXISTS_CHILDREN);
    }

    @Test
    void deleteMenu_不存在() {
        assertServiceException(() -> menuService.deleteMenu(-1L), MENU_NOT_EXISTS);
    }

    // ========== 查询 ==========

    @Test
    void getMenuList_按名称与状态过滤() {
        insert("用户管理", 0L, MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);
        insert("用户导入", 0L, MenuTypeEnum.MENU, null, CommonStatusEnum.DISABLE);
        insert("角色管理", 0L, MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);

        MenuListReqVO reqVO = new MenuListReqVO();
        reqVO.setName("用户");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        assertThat(menuService.getMenuList(reqVO)).extracting(MenuDO::getName).containsExactly("用户管理");
    }

    @Test
    void getMenuListByTenant_按租户套餐过滤() {
        MenuDO inPackage = insert("套餐内", 0L, MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);
        insert("套餐外", 0L, MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);
        doAnswer(inv -> {
            inv.<TenantMenuHandler>getArgument(0).handle(Set.of(inPackage.getId()));
            return null;
        }).when(tenantService).handleTenantMenu(any());

        List<MenuDO> menus = menuService.getMenuListByTenant(new MenuListReqVO());

        assertThat(menus).extracting(MenuDO::getName).containsExactly("套餐内");
    }

    @Test
    void getMenuListByTenant_系统租户不过滤() {
        insert("甲", 0L, MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);
        insert("乙", 0L, MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);
        // 系统租户时 handleTenantMenu 不回调
        assertThat(menuService.getMenuListByTenant(new MenuListReqVO())).hasSize(2);
    }

    @Test
    void filterDisableMenus_祖先停用时子孙一并移除() {
        MenuDO disabledDir = insert("停用目录", 0L, MenuTypeEnum.DIR, null, CommonStatusEnum.DISABLE);
        MenuDO child = insert("子页面", disabledDir.getId(), MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);
        insert("按钮", child.getId(), MenuTypeEnum.BUTTON, "x:y", CommonStatusEnum.ENABLE);
        MenuDO okDir = insert("正常目录", 0L, MenuTypeEnum.DIR, null, CommonStatusEnum.ENABLE);
        insert("正常页面", okDir.getId(), MenuTypeEnum.MENU, null, CommonStatusEnum.ENABLE);

        List<MenuDO> result = menuService.filterDisableMenus(menuService.getMenuList());

        assertThat(result).extracting(MenuDO::getName).containsExactlyInAnyOrder("正常目录", "正常页面");
    }

    // ========== 工具 ==========

    private MenuDO insert(String name, Long parentId, MenuTypeEnum type, String permission, CommonStatusEnum status) {
        MenuDO menu = new MenuDO().setName(name).setParentId(parentId).setType(type.getType())
                .setPermission(permission == null ? "" : permission).setSort(0).setStatus(status.getStatus());
        menuMapper.insert(menu);
        return menu;
    }

    private static MenuSaveVO newSaveVO(String name, Long parentId, MenuTypeEnum type, String permission) {
        MenuSaveVO reqVO = new MenuSaveVO();
        reqVO.setName(name);
        reqVO.setParentId(parentId);
        reqVO.setType(type.getType());
        reqVO.setPermission(permission);
        reqVO.setSort(1);
        reqVO.setPath("/path");
        reqVO.setIcon("user");
        reqVO.setComponent("system/user/index");
        reqVO.setComponentName("SystemUser");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }
}
