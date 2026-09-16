package com.ccloud.module.system.service.user;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.user.AdminUserMapper;
import com.ccloud.module.system.service.tenant.TenantService;
import com.ccloud.module.system.service.tenant.TenantInfoHandler;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@Import(AdminUserServiceImpl.class)
class AdminUserServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private AdminUserServiceImpl userService;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TenantService tenantService;

    @Test
    void createUser_成功时密码加密存储且默认开启() {
        Long id = userService.createUser(newReqVO("zhangsan", "13700000001", "zs@ccloud.local"));

        AdminUserDO user = userMapper.selectById(id);
        assertThat(user.getUsername()).isEqualTo("zhangsan");
        assertThat(user.getPassword()).isNotEqualTo("pass123");
        assertThat(passwordEncoder.matches("pass123", user.getPassword())).isTrue();
        assertThat(user.getStatus()).isEqualTo(CommonStatusEnum.ENABLE.getStatus());
        assertThat(user.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createUser_账号重复() {
        userService.createUser(newReqVO("dup", null, null));
        assertServiceException(() -> userService.createUser(newReqVO("dup", null, null)), USER_USERNAME_EXISTS);
    }

    @Test
    void createUser_手机号重复() {
        userService.createUser(newReqVO("a1", "13700000002", null));
        assertServiceException(() -> userService.createUser(newReqVO("a2", "13700000002", null)), USER_MOBILE_EXISTS);
    }

    @Test
    void createUser_邮箱重复() {
        userService.createUser(newReqVO("b1", null, "b@ccloud.local"));
        assertServiceException(() -> userService.createUser(newReqVO("b2", null, "b@ccloud.local")), USER_EMAIL_EXISTS);
    }

    @Test
    void createUser_空手机号与空邮箱不参与唯一性校验() {
        userService.createUser(newReqVO("c1", null, null));
        userService.createUser(newReqVO("c2", null, null));
        assertThat(userMapper.selectCount()).isEqualTo(2);
    }

    @Test
    void createUser_超过租户账号配额() {
        doAnswer(inv -> {
            inv.<TenantInfoHandler>getArgument(0).handle(new TenantDO().setAccountCount(1));
            return null;
        }).when(tenantService).handleTenantInfo(any());
        userService.createUser(newReqVO("q1", null, null));

        assertServiceException(() -> userService.createUser(newReqVO("q2", null, null)), USER_COUNT_MAX);
    }

    private static UserSaveReqVO newReqVO(String username, String mobile, String email) {
        UserSaveReqVO reqVO = new UserSaveReqVO();
        reqVO.setUsername(username);
        reqVO.setNickname("昵称");
        reqVO.setPassword("pass123");
        reqVO.setMobile(mobile);
        reqVO.setEmail(email);
        return reqVO;
    }
}
