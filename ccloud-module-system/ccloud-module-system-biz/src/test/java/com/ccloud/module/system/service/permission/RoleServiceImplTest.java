package com.ccloud.module.system.service.permission;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import com.ccloud.module.system.dal.mysql.permission.RoleMapper;
import com.ccloud.module.system.enums.permission.DataScopeEnum;
import com.ccloud.module.system.enums.permission.RoleTypeEnum;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import(RoleServiceImpl.class)
class RoleServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private RoleServiceImpl roleService;
    @Resource
    private RoleMapper roleMapper;

    @Test
    void createRole_成功时默认开启且数据范围为全部() {
        Long id = roleService.createRole(newReqVO("运营", "operator"), RoleTypeEnum.CUSTOM.getType());

        RoleDO role = roleMapper.selectById(id);
        assertThat(role.getName()).isEqualTo("运营");
        assertThat(role.getType()).isEqualTo(RoleTypeEnum.CUSTOM.getType());
        assertThat(role.getStatus()).isEqualTo(CommonStatusEnum.ENABLE.getStatus());
        assertThat(role.getDataScope()).isEqualTo(DataScopeEnum.ALL.getScope());
        assertThat(role.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createRole_名称重复() {
        roleService.createRole(newReqVO("运营", "operator"), null);
        assertServiceException(() -> roleService.createRole(newReqVO("运营", "operator2"), null), ROLE_NAME_DUPLICATE);
    }

    @Test
    void createRole_标识重复() {
        roleService.createRole(newReqVO("运营", "operator"), null);
        assertServiceException(() -> roleService.createRole(newReqVO("运营2", "operator"), null), ROLE_CODE_DUPLICATE);
    }

    @Test
    void createRole_不允许使用超级管理员标识() {
        assertServiceException(() -> roleService.createRole(newReqVO("假冒", "super_admin"), null), ROLE_ADMIN_CODE_ERROR);
    }

    @Test
    void createRole_不同租户可以同名() {
        roleService.createRole(newReqVO("运营", "operator"), null);
        Long otherId = TenantUtils.execute(2L, () -> roleService.createRole(newReqVO("运营", "operator"), null));
        assertThat(otherId).isNotNull();
    }

    @Test
    void getRole与getRoleList只返回当前租户数据() {
        Long mine = roleService.createRole(newReqVO("本租户", "mine"), null);
        TenantUtils.execute(2L, () -> roleService.createRole(newReqVO("他租户", "other"), null));

        assertThat(roleService.getRole(mine).getName()).isEqualTo("本租户");
        assertThat(roleService.getRoleList()).extracting(RoleDO::getName).containsExactly("本租户");
    }

    private static RoleSaveReqVO newReqVO(String name, String code) {
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setName(name);
        reqVO.setCode(code);
        reqVO.setSort(1);
        reqVO.setRemark("");
        return reqVO;
    }
}
