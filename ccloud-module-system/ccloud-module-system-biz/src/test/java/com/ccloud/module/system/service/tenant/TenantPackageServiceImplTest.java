package com.ccloud.module.system.service.tenant;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.tenant.vo.packages.TenantPackagePageReqVO;
import com.ccloud.module.system.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantPackageDO;
import com.ccloud.module.system.dal.mysql.tenant.TenantPackageMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@Import(TenantPackageServiceImpl.class)
class TenantPackageServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private TenantPackageServiceImpl tenantPackageService;
    @Resource
    private TenantPackageMapper tenantPackageMapper;

    @MockitoBean
    private TenantService tenantService;

    @Test
    void createTenantPackage_成功() {
        Long id = tenantPackageService.createTenantPackage(newSaveReqVO("高级版", Set.of(1L, 2L)));

        TenantPackageDO pkg = tenantPackageMapper.selectById(id);
        assertThat(pkg.getName()).isEqualTo("高级版");
        assertThat(pkg.getMenuIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void createTenantPackage_名称重复() {
        insert("重名版", Set.of(1L), CommonStatusEnum.ENABLE);
        assertServiceException(() -> tenantPackageService.createTenantPackage(newSaveReqVO("重名版", Set.of(1L))),
                TENANT_PACKAGE_NAME_DUPLICATE);
    }

    @Test
    void updateTenantPackage_菜单变化时同步到使用该套餐的每个租户() {
        TenantPackageDO pkg = insert("同步版", Set.of(1L, 2L), CommonStatusEnum.ENABLE);
        when(tenantService.getTenantListByPackageId(pkg.getId())).thenReturn(List.of(
                new TenantDO().setId(101L), new TenantDO().setId(102L)));

        TenantPackageSaveReqVO reqVO = newSaveReqVO("同步版", Set.of(2L, 3L));
        reqVO.setId(pkg.getId());
        tenantPackageService.updateTenantPackage(reqVO);

        assertThat(tenantPackageMapper.selectById(pkg.getId()).getMenuIds()).containsExactlyInAnyOrder(2L, 3L);
        verify(tenantService).updateTenantRoleMenu(101L, Set.of(2L, 3L));
        verify(tenantService).updateTenantRoleMenu(102L, Set.of(2L, 3L));
    }

    @Test
    void updateTenantPackage_菜单未变化时不触发同步() {
        TenantPackageDO pkg = insert("不变版", Set.of(1L, 2L), CommonStatusEnum.ENABLE);

        TenantPackageSaveReqVO reqVO = newSaveReqVO("不变版-改名", Set.of(2L, 1L));
        reqVO.setId(pkg.getId());
        tenantPackageService.updateTenantPackage(reqVO);

        assertThat(tenantPackageMapper.selectById(pkg.getId()).getName()).isEqualTo("不变版-改名");
        verify(tenantService, never()).updateTenantRoleMenu(anyLong(), any());
    }

    @Test
    void updateTenantPackage_不存在() {
        TenantPackageSaveReqVO reqVO = newSaveReqVO("x", Set.of(1L));
        reqVO.setId(-1L);
        assertServiceException(() -> tenantPackageService.updateTenantPackage(reqVO), TENANT_PACKAGE_NOT_EXISTS);
    }

    @Test
    void updateTenantPackage_名称与其他套餐重复() {
        insert("占用版", Set.of(1L), CommonStatusEnum.ENABLE);
        TenantPackageDO self = insert("自身版", Set.of(1L), CommonStatusEnum.ENABLE);
        TenantPackageSaveReqVO reqVO = newSaveReqVO("占用版", Set.of(1L));
        reqVO.setId(self.getId());
        assertServiceException(() -> tenantPackageService.updateTenantPackage(reqVO), TENANT_PACKAGE_NAME_DUPLICATE);
    }

    @Test
    void deleteTenantPackage_成功() {
        TenantPackageDO pkg = insert("待删版", Set.of(1L), CommonStatusEnum.ENABLE);
        tenantPackageService.deleteTenantPackage(pkg.getId());
        assertThat(tenantPackageMapper.selectById(pkg.getId())).isNull();
    }

    @Test
    void deleteTenantPackage_不存在() {
        assertServiceException(() -> tenantPackageService.deleteTenantPackage(-1L), TENANT_PACKAGE_NOT_EXISTS);
    }

    @Test
    void deleteTenantPackage_仍被租户使用() {
        TenantPackageDO pkg = insert("在用版", Set.of(1L), CommonStatusEnum.ENABLE);
        when(tenantService.getTenantCountByPackageId(pkg.getId())).thenReturn(1L);
        assertServiceException(() -> tenantPackageService.deleteTenantPackage(pkg.getId()), TENANT_PACKAGE_USED);
        assertThat(tenantPackageMapper.selectById(pkg.getId())).isNotNull();
    }

    @Test
    void validTenantPackage_三个分支() {
        TenantPackageDO enabled = insert("可用版", Set.of(1L), CommonStatusEnum.ENABLE);
        TenantPackageDO disabled = insert("停用版", Set.of(1L), CommonStatusEnum.DISABLE);

        assertThat(tenantPackageService.validTenantPackage(enabled.getId()).getId()).isEqualTo(enabled.getId());
        assertServiceException(() -> tenantPackageService.validTenantPackage(disabled.getId()), TENANT_PACKAGE_DISABLE);
        assertServiceException(() -> tenantPackageService.validTenantPackage(-1L), TENANT_PACKAGE_NOT_EXISTS);
    }

    @Test
    void getTenantPackagePage_按条件过滤() {
        insert("旗舰版A", Set.of(1L), CommonStatusEnum.ENABLE);
        insert("旗舰版B", Set.of(1L), CommonStatusEnum.DISABLE);
        insert("基础版", Set.of(1L), CommonStatusEnum.ENABLE);

        TenantPackagePageReqVO reqVO = new TenantPackagePageReqVO();
        reqVO.setName("旗舰");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        PageResult<TenantPackageDO> page = tenantPackageService.getTenantPackagePage(reqVO);

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).extracting(TenantPackageDO::getName).containsExactly("旗舰版A");
    }

    @Test
    void getTenantPackageListByStatus() {
        TenantPackageDO enabled = insert("列表可用", Set.of(1L), CommonStatusEnum.ENABLE);
        TenantPackageDO disabled = insert("列表停用", Set.of(1L), CommonStatusEnum.DISABLE);

        List<TenantPackageDO> list = tenantPackageService.getTenantPackageListByStatus(CommonStatusEnum.ENABLE.getStatus());

        assertThat(list).extracting(TenantPackageDO::getId).contains(enabled.getId()).doesNotContain(disabled.getId());
    }

    private TenantPackageDO insert(String name, Set<Long> menuIds, CommonStatusEnum status) {
        TenantPackageDO pkg = new TenantPackageDO().setName(name).setStatus(status.getStatus())
                .setRemark("").setMenuIds(menuIds);
        tenantPackageMapper.insert(pkg);
        return pkg;
    }

    private static TenantPackageSaveReqVO newSaveReqVO(String name, Set<Long> menuIds) {
        TenantPackageSaveReqVO reqVO = new TenantPackageSaveReqVO();
        reqVO.setName(name);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setRemark("备注");
        reqVO.setMenuIds(menuIds);
        return reqVO;
    }
}
