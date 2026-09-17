package com.ccloud.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.security.core.service.PermissionCacheRefresher;
import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuListReqVO;
import com.ccloud.module.system.controller.admin.permission.vo.menu.MenuSaveVO;
import com.ccloud.module.system.dal.dataobject.permission.MenuDO;
import com.ccloud.module.system.dal.mysql.permission.MenuMapper;
import com.ccloud.module.system.enums.permission.MenuTypeEnum;
import com.ccloud.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.dal.redis.RedisKeyConstants.PERMISSION_MENU_ID_LIST;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class MenuServiceImpl implements MenuService {

    @Resource
    private MenuMapper menuMapper;
    @Resource
    @Lazy
    private PermissionService permissionService;
    @Resource
    @Lazy
    private TenantService tenantService;
    @Resource
    private ObjectProvider<PermissionCacheRefresher> permissionCacheRefresherProvider;

    // ========== 增删改 ==========

    @Override
    @CacheEvict(value = PERMISSION_MENU_ID_LIST, allEntries = true)
    public Long createMenu(MenuSaveVO createReqVO) {
        validateSystemTenant();
        validateParentMenu(createReqVO.getParentId(), null);
        validateMenuNameUnique(createReqVO.getName(), createReqVO.getParentId(), null);

        MenuDO menu = BeanUtils.toBean(createReqVO, MenuDO.class);
        initMenuProperty(menu);
        menuMapper.insert(menu);
        refreshPermissionCache();
        return menu.getId();
    }

    @Override
    @CacheEvict(value = PERMISSION_MENU_ID_LIST, allEntries = true)
    public void updateMenu(MenuSaveVO updateReqVO) {
        validateSystemTenant();
        if (menuMapper.selectById(updateReqVO.getId()) == null) {
            throw exception(MENU_NOT_EXISTS);
        }
        validateParentMenu(updateReqVO.getParentId(), updateReqVO.getId());
        validateMenuNameUnique(updateReqVO.getName(), updateReqVO.getParentId(), updateReqVO.getId());

        MenuDO updateObj = BeanUtils.toBean(updateReqVO, MenuDO.class);
        initMenuProperty(updateObj);
        menuMapper.updateById(updateObj);
        refreshPermissionCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = PERMISSION_MENU_ID_LIST, allEntries = true)
    public void deleteMenu(Long id) {
        validateSystemTenant();
        if (menuMapper.selectById(id) == null) {
            throw exception(MENU_NOT_EXISTS);
        }
        if (menuMapper.selectCountByParentId(id) > 0) {
            throw exception(MENU_EXISTS_CHILDREN);
        }
        menuMapper.deleteById(id);
        permissionService.processMenuDeleted(id);
        refreshPermissionCache();
    }

    /**
     * 按"子先父后"的顺序删除，使同时选中父子菜单的批量删除可以成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = PERMISSION_MENU_ID_LIST, allEntries = true)
    public void deleteMenuList(List<Long> ids) {
        List<MenuDO> menus = menuMapper.selectByIds(ids);
        Map<Long, Integer> depth = new HashMap<>();
        Map<Long, MenuDO> all = CollectionUtils.convertMap(menuMapper.selectList(), MenuDO::getId);
        menus.forEach(menu -> depth.put(menu.getId(), depthOf(menu, all)));
        ids.stream().sorted(Comparator.comparing((Long id) -> depth.getOrDefault(id, 0)).reversed())
                .forEach(this::deleteMenu);
    }

    private static int depthOf(MenuDO menu, Map<Long, MenuDO> all) {
        int depth = 0;
        MenuDO current = menu;
        while (current != null && !MenuDO.ID_ROOT.equals(current.getParentId()) && depth < all.size()) {
            current = all.get(current.getParentId());
            depth++;
        }
        return depth;
    }

    /**
     * 菜单为全部租户共用，普通租户（即使套餐里包含菜单管理权限）也不能修改，否则会影响其他租户。
     * handleTenantMenu 只对受套餐限制的租户回调，借此判断当前是否为系统租户
     */
    private void validateSystemTenant() {
        tenantService.handleTenantMenu(menuIds -> {
            throw exception(MENU_ONLY_SYSTEM_TENANT);
        });
    }

    private void refreshPermissionCache() {
        permissionCacheRefresherProvider.ifAvailable(refresher -> refresher.publish(null));
    }

    /**
     * 父菜单须存在且为目录或菜单；不能是自己，也不能是自己的子孙（否则形成环，菜单树无法构建）
     */
    private void validateParentMenu(Long parentId, Long childId) {
        if (parentId == null || MenuDO.ID_ROOT.equals(parentId)) {
            return;
        }
        if (parentId.equals(childId)) {
            throw exception(MENU_PARENT_ERROR);
        }
        MenuDO parent = menuMapper.selectById(parentId);
        if (parent == null) {
            throw exception(MENU_PARENT_NOT_EXISTS);
        }
        if (!MenuTypeEnum.DIR.getType().equals(parent.getType())
                && !MenuTypeEnum.MENU.getType().equals(parent.getType())) {
            throw exception(MENU_PARENT_NOT_DIR_OR_MENU);
        }
        if (childId != null) {
            Map<Long, MenuDO> all = CollectionUtils.convertMap(menuMapper.selectList(), MenuDO::getId);
            MenuDO ancestor = parent;
            for (int i = 0; ancestor != null && i <= all.size(); i++) {
                if (childId.equals(ancestor.getId())) {
                    throw exception(MENU_PARENT_ERROR);
                }
                ancestor = all.get(ancestor.getParentId());
            }
        }
    }

    /**
     * 同一父菜单下名称唯一
     */
    private void validateMenuNameUnique(String name, Long parentId, Long id) {
        MenuDO menu = menuMapper.selectByParentIdAndName(parentId, name);
        if (menu != null && !menu.getId().equals(id)) {
            throw exception(MENU_NAME_DUPLICATE);
        }
    }

    /**
     * 按钮不参与路由：清空路由与组件相关属性，避免前端误把按钮当成页面
     */
    private static void initMenuProperty(MenuDO menu) {
        if (MenuTypeEnum.BUTTON.getType().equals(menu.getType())) {
            menu.setPath("");
            menu.setIcon("");
            menu.setComponent("");
            menu.setComponentName("");
        }
        if (menu.getPermission() == null) {
            menu.setPermission("");
        }
    }

    // ========== 查询 ==========

    @Override
    public MenuDO getMenu(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public List<MenuDO> getMenuList() {
        return menuMapper.selectList();
    }

    @Override
    public List<MenuDO> getMenuList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return menuMapper.selectByIds(ids);
    }

    @Override
    public List<MenuDO> getMenuList(MenuListReqVO reqVO) {
        return menuMapper.selectList(reqVO);
    }

    @Override
    public List<MenuDO> getMenuListByTenant(MenuListReqVO reqVO) {
        List<MenuDO> menus = new ArrayList<>(getMenuList(reqVO));
        tenantService.handleTenantMenu(menuIds -> menus.removeIf(menu -> !menuIds.contains(menu.getId())));
        return menus;
    }

    @Override
    public List<MenuDO> filterDisableMenus(List<MenuDO> menus) {
        if (CollUtil.isEmpty(menus)) {
            return Collections.emptyList();
        }
        Map<Long, MenuDO> menuMap = CollectionUtils.convertMap(menus, MenuDO::getId);
        // 结果缓存：menuId → 自身及全部祖先是否都开启
        Map<Long, Boolean> enabled = new HashMap<>();
        List<MenuDO> result = new ArrayList<>(menus.size());
        for (MenuDO menu : menus) {
            if (isEnabledWithAncestors(menu, menuMap, enabled)) {
                result.add(menu);
            }
        }
        return result;
    }

    private static boolean isEnabledWithAncestors(MenuDO menu, Map<Long, MenuDO> menuMap, Map<Long, Boolean> cache) {
        Deque<MenuDO> path = new ArrayDeque<>();
        Boolean known = null;
        MenuDO current = menu;
        while (current != null && path.size() <= menuMap.size()) {
            known = cache.get(current.getId());
            if (known != null) {
                break;
            }
            path.push(current);
            if (!CommonStatusEnum.isEnable(current.getStatus())) {
                known = false;
                break;
            }
            current = MenuDO.ID_ROOT.equals(current.getParentId()) ? null : menuMap.get(current.getParentId());
        }
        boolean value = known == null || known;
        path.forEach(m -> cache.put(m.getId(), value));
        return value;
    }

    @Override
    @Cacheable(value = PERMISSION_MENU_ID_LIST, key = "#permission")
    public List<Long> getMenuIdListByPermissionFromCache(String permission) {
        List<MenuDO> menus = menuMapper.selectListByPermission(permission);
        return CollectionUtils.convertList(menus, MenuDO::getId, menu -> CommonStatusEnum.isEnable(menu.getStatus()));
    }
}
