package com.ccloud.module.system.service.tenant;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import com.ccloud.module.system.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import com.ccloud.module.system.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import com.ccloud.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantPackageDO;
import com.ccloud.module.system.dal.mysql.tenant.TenantMapper;
import com.ccloud.module.system.enums.permission.RoleCodeEnum;
import com.ccloud.module.system.enums.permission.RoleTypeEnum;
import com.ccloud.module.system.service.permission.PermissionService;
import com.ccloud.module.system.service.permission.RoleService;
import com.ccloud.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class TenantServiceImpl implements TenantService {

    @Resource
    private TenantMapper tenantMapper;

    /**
     * 与套餐、用户 Service 互相依赖，延迟注入打破循环
     */
    @Resource
    @Lazy
    private TenantPackageService tenantPackageService;
    @Resource
    @Lazy
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private ObjectProvider<TenantProperties> tenantPropertiesProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTenant(TenantSaveReqVO createReqVO) {
        validateTenantNameDuplicate(createReqVO.getName(), null);
        validateTenantWebsiteDuplicate(createReqVO.getWebsite(), null);
        TenantPackageDO tenantPackage = tenantPackageService.validTenantPackage(createReqVO.getPackageId());

        TenantDO tenant = BeanUtils.toBean(createReqVO, TenantDO.class);
        tenantMapper.insert(tenant);

        // 角色、账号属于新租户，必须切换到新租户上下文写入
        TenantUtils.execute(tenant.getId(), () -> {
            Long roleId = createTenantAdminRole(tenantPackage);
            Long userId = createTenantAdminUser(roleId, createReqVO);
            tenantMapper.updateById(new TenantDO().setId(tenant.getId()).setContactUserId(userId));
        });
        return tenant.getId();
    }

    private Long createTenantAdminRole(TenantPackageDO tenantPackage) {
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setName(RoleCodeEnum.TENANT_ADMIN.getName());
        reqVO.setCode(RoleCodeEnum.TENANT_ADMIN.getCode());
        reqVO.setSort(0);
        reqVO.setRemark("创建租户时自动生成");
        Long roleId = roleService.createRole(reqVO, RoleTypeEnum.SYSTEM.getType());
        permissionService.assignRoleMenu(roleId, tenantPackage.getMenuIds());
        return roleId;
    }

    private Long createTenantAdminUser(Long roleId, TenantSaveReqVO createReqVO) {
        UserSaveReqVO reqVO = new UserSaveReqVO();
        reqVO.setUsername(createReqVO.getUsername());
        reqVO.setPassword(createReqVO.getPassword());
        reqVO.setNickname(createReqVO.getContactName());
        reqVO.setMobile(createReqVO.getContactMobile());
        Long userId = userService.createUser(reqVO);
        permissionService.assignUserRole(userId, Collections.singleton(roleId));
        return userId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenant(TenantSaveReqVO updateReqVO) {
        TenantDO tenant = validateUpdateTenant(updateReqVO.getId());
        validateTenantNameDuplicate(updateReqVO.getName(), updateReqVO.getId());
        validateTenantWebsiteDuplicate(updateReqVO.getWebsite(), updateReqVO.getId());
        TenantPackageDO tenantPackage = tenantPackageService.validTenantPackage(updateReqVO.getPackageId());

        TenantDO updateObj = BeanUtils.toBean(updateReqVO, TenantDO.class);
        tenantMapper.updateById(updateObj);

        if (!Objects.equals(tenant.getPackageId(), updateReqVO.getPackageId())) {
            updateTenantRoleMenu(tenant.getId(), tenantPackage.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenantRoleMenu(Long tenantId, Set<Long> menuIds) {
        TenantUtils.execute(tenantId, () -> {
            List<RoleDO> roles = roleService.getRoleList();
            for (RoleDO role : roles) {
                if (RoleCodeEnum.TENANT_ADMIN.getCode().equals(role.getCode())) {
                    permissionService.assignRoleMenu(role.getId(), menuIds);
                    log.info("[updateTenantRoleMenu][租户({}) 管理员角色({}) 菜单同步为 {}]", tenantId, role.getId(), menuIds);
                    continue;
                }
                // 其他角色：只收回超出范围的菜单，不擅自授予新菜单
                Set<Long> roleMenuIds = permissionService.getRoleMenuListByRoleId(role.getId());
                Set<Long> retained = CollectionUtils.convertSet(roleMenuIds, id -> id);
                retained.retainAll(menuIds);
                if (retained.size() != roleMenuIds.size()) {
                    permissionService.assignRoleMenu(role.getId(), retained);
                    log.info("[updateTenantRoleMenu][租户({}) 角色({}) 菜单收窄为 {}]", tenantId, role.getId(), retained);
                }
            }
        });
    }

    @Override
    public void deleteTenant(Long id) {
        validateUpdateTenant(id);
        tenantMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTenantList(List<Long> ids) {
        ids.forEach(this::validateUpdateTenant);
        tenantMapper.deleteByIds(ids);
    }

    private TenantDO validateUpdateTenant(Long id) {
        TenantDO tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw exception(TENANT_NOT_EXISTS);
        }
        if (tenant.isSystem()) {
            throw exception(TENANT_CAN_NOT_UPDATE_SYSTEM);
        }
        return tenant;
    }

    private void validateTenantNameDuplicate(String name, Long id) {
        TenantDO tenant = tenantMapper.selectByName(name);
        if (tenant != null && !tenant.getId().equals(id)) {
            throw exception(TENANT_NAME_DUPLICATE, name);
        }
    }

    private void validateTenantWebsiteDuplicate(String website, Long id) {
        if (StrUtil.isEmpty(website)) {
            return;
        }
        TenantDO tenant = tenantMapper.selectByWebsite(website);
        if (tenant != null && !tenant.getId().equals(id)) {
            throw exception(TENANT_WEBSITE_DUPLICATE, website);
        }
    }

    @Override
    public TenantDO getTenant(Long id) {
        return tenantMapper.selectById(id);
    }

    @Override
    public PageResult<TenantDO> getTenantPage(TenantPageReqVO pageReqVO) {
        return tenantMapper.selectPage(pageReqVO);
    }

    @Override
    public TenantDO getTenantByName(String name) {
        return tenantMapper.selectByName(name);
    }

    @Override
    public TenantDO getTenantByWebsite(String website) {
        return tenantMapper.selectByWebsite(website);
    }

    @Override
    public Long getTenantCountByPackageId(Long packageId) {
        return tenantMapper.selectCountByPackageId(packageId);
    }

    @Override
    public List<TenantDO> getTenantListByPackageId(Long packageId) {
        return tenantMapper.selectListByPackageId(packageId);
    }

    @Override
    public List<TenantDO> getTenantListByStatus(Integer status) {
        return tenantMapper.selectListByStatus(status);
    }

    @Override
    public void handleTenantInfo(TenantInfoHandler handler) {
        if (isTenantDisable()) {
            return;
        }
        handler.handle(getTenant(TenantContextHolder.getRequiredTenantId()));
    }

    @Override
    public void handleTenantMenu(TenantMenuHandler handler) {
        if (isTenantDisable()) {
            return;
        }
        TenantDO tenant = getTenant(TenantContextHolder.getRequiredTenantId());
        if (tenant == null || tenant.isSystem()) {
            return;
        }
        TenantPackageDO tenantPackage = tenantPackageService.getTenantPackage(tenant.getPackageId());
        handler.handle(tenantPackage != null ? tenantPackage.getMenuIds() : Collections.emptySet());
    }

    @Override
    public List<Long> getTenantIdList() {
        List<TenantDO> tenants = tenantMapper.selectListByStatusAndExpireAfter(
                CommonStatusEnum.ENABLE.getStatus(), LocalDateTime.now());
        return CollectionUtils.convertList(tenants, TenantDO::getId);
    }

    @Override
    public void validTenant(Long id) {
        TenantDO tenant = getTenant(id);
        if (tenant == null) {
            throw exception(TENANT_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(tenant.getStatus())) {
            throw exception(TENANT_DISABLE, tenant.getName());
        }
        if (tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw exception(TENANT_EXPIRE, tenant.getName());
        }
    }

    private boolean isTenantDisable() {
        TenantProperties properties = tenantPropertiesProvider.getIfAvailable();
        return properties == null || !Boolean.TRUE.equals(properties.getEnable());
    }
}
