package com.ccloud.module.member.service.user;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.TerminalEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.member.controller.app.user.vo.AppMemberUserUpdateMobileReqVO;
import com.ccloud.module.member.controller.app.user.vo.AppMemberUserUpdateReqVO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelDO;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.service.level.MemberLevelService;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import com.ccloud.module.system.api.sms.SmsCodeApi;
import com.ccloud.module.system.api.sms.dto.SmsCodeUseReqDTO;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static com.ccloud.module.member.enums.ErrorCodeConstants.USER_MOBILE_USED;
import static com.ccloud.module.member.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import static com.ccloud.module.system.enums.ErrorCodeConstants.SMS_CODE_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@Import(MemberUserServiceImpl.class)
class MemberUserServiceImplTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberUserServiceImpl userService;
    @Resource
    private MemberUserMapper userMapper;

    @MockitoBean
    private SmsCodeApi smsCodeApi;
    @MockitoBean
    private MemberLevelService levelService;

    @Test
    void createUserIfAbsent_新会员按0经验定初始等级_积分经验为0() {
        when(levelService.calculateLevel(0)).thenReturn(new MemberLevelDO().setId(7L));
        MemberUserDO user = userService.createUserIfAbsent("13911110101", "10.0.0.1", null);
        MemberUserDO saved = userMapper.selectById(user.getId());
        assertThat(saved.getLevelId()).isEqualTo(7L);
        assertThat(saved.getPoint()).isZero();
        assertThat(saved.getExperience()).isZero();
    }

    @Test
    void createUserIfAbsent_没有满足的等级时等级为空() {
        when(levelService.calculateLevel(0)).thenReturn(null);
        MemberUserDO user = userService.createUserIfAbsent("13911110102", "10.0.0.1", null);
        assertThat(userMapper.selectById(user.getId()).getLevelId()).isNull();
    }

    @Test
    void createUserIfAbsent_新手机号应注册_记录注册IP与终端_昵称不暴露完整手机号_无密码() {
        MemberUserDO user = userService.createUserIfAbsent("13911110001", "10.0.0.1",
                TerminalEnum.WECHAT_MINI_PROGRAM.getTerminal());

        MemberUserDO saved = userMapper.selectById(user.getId());
        assertThat(saved.getMobile()).isEqualTo("13911110001");
        assertThat(saved.getRegisterIp()).isEqualTo("10.0.0.1");
        assertThat(saved.getRegisterTerminal()).isEqualTo(TerminalEnum.WECHAT_MINI_PROGRAM.getTerminal());
        assertThat(saved.getStatus()).isEqualTo(CommonStatusEnum.ENABLE.getStatus());
        assertThat(saved.getNickname()).isNotBlank().doesNotContain("13911110001").endsWith("0001");
        assertThat(saved.getPassword()).isEmpty();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createUserIfAbsent_手机号已注册时应返回原用户_不重复创建() {
        MemberUserDO first = userService.createUserIfAbsent("13911110002", "10.0.0.1", null);
        MemberUserDO second = userService.createUserIfAbsent("13911110002", "10.0.0.2", TerminalEnum.APP.getTerminal());

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(userMapper.selectList(MemberUserDO::getMobile, "13911110002")).hasSize(1);
        assertThat(userMapper.selectById(first.getId()).getRegisterIp()).isEqualTo("10.0.0.1");
    }

    @Test
    void getUserByMobile_不同租户的同一手机号互不可见() {
        userService.createUserIfAbsent("13911110003", "10.0.0.1", null);

        MemberUserDO otherTenant = TenantUtils.execute(TENANT_ID + 1000, () -> userService.getUserByMobile("13911110003"));

        assertThat(otherTenant).isNull();
        assertThat(userService.getUserByMobile("13911110003")).isNotNull();
    }

    @Test
    void updateUserPassword_应以BCrypt存储且可校验() {
        MemberUserDO user = userService.createUserIfAbsent("13911110004", "10.0.0.1", null);

        userService.updateUserPassword(user.getId(), "new123");

        String encoded = userMapper.selectById(user.getId()).getPassword();
        assertThat(encoded).startsWith("$2").isNotEqualTo("new123");
        assertThat(userService.isPasswordMatch("new123", encoded)).isTrue();
        assertThat(userService.isPasswordMatch("wrong", encoded)).isFalse();
    }

    @Test
    void isPasswordMatch_未设置密码的用户任何密码都不匹配() {
        assertThat(userService.isPasswordMatch("", "")).isFalse();
        assertThat(userService.isPasswordMatch("anything", "")).isFalse();
    }

    @Test
    void updateUser_只更新非空字段() {
        MemberUserDO user = userService.createUserIfAbsent("13911110005", "10.0.0.1", null);
        String nickname = userMapper.selectById(user.getId()).getNickname();
        AppMemberUserUpdateReqVO reqVO = new AppMemberUserUpdateReqVO();
        reqVO.setSex(1);

        userService.updateUser(user.getId(), reqVO);

        MemberUserDO saved = userMapper.selectById(user.getId());
        assertThat(saved.getSex()).isEqualTo(1);
        assertThat(saved.getNickname()).isEqualTo(nickname);
    }

    @Test
    void updateUserMobile_新旧验证码都通过时应更新手机号() {
        MemberUserDO user = userService.createUserIfAbsent("13911110006", "10.0.0.1", null);
        when(smsCodeApi.useSmsCode(any())).thenReturn(CommonResult.success(true));

        userService.updateUserMobile(user.getId(), updateMobileReqVO("13911110007"), "10.0.0.9");

        assertThat(userMapper.selectById(user.getId()).getMobile()).isEqualTo("13911110007");
        ArgumentCaptor<SmsCodeUseReqDTO> captor = ArgumentCaptor.forClass(SmsCodeUseReqDTO.class);
        verify(smsCodeApi, times(2)).useSmsCode(captor.capture());
        assertThat(captor.getAllValues()).extracting(SmsCodeUseReqDTO::getMobile, SmsCodeUseReqDTO::getCode)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("13911110006", "old-code"),
                        org.assertj.core.groups.Tuple.tuple("13911110007", "new-code"));
        assertThat(captor.getAllValues()).allSatisfy(dto -> {
            assertThat(dto.getScene()).isEqualTo(SmsSceneEnum.MEMBER_UPDATE_MOBILE.getScene());
            assertThat(dto.getUsedIp()).isEqualTo("10.0.0.9");
        });
    }

    @Test
    void updateUserMobile_原手机验证码错误时不应更新() {
        MemberUserDO user = userService.createUserIfAbsent("13911110008", "10.0.0.1", null);
        when(smsCodeApi.useSmsCode(argThat(dto -> dto != null && "13911110008".equals(dto.getMobile()))))
                .thenReturn(CommonResult.error(SMS_CODE_NOT_FOUND));
        when(smsCodeApi.useSmsCode(argThat(dto -> dto != null && "13911110009".equals(dto.getMobile()))))
                .thenReturn(CommonResult.success(true));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        userService.updateUserMobile(user.getId(), updateMobileReqVO("13911110009"), "10.0.0.9"))
                .extracting("code").isEqualTo(SMS_CODE_NOT_FOUND.getCode());
        assertThat(userMapper.selectById(user.getId()).getMobile()).isEqualTo("13911110008");
    }

    @Test
    void updateUserMobile_新手机号已被其他用户使用时应失败_且不消耗验证码() {
        MemberUserDO user = userService.createUserIfAbsent("13911110010", "10.0.0.1", null);
        userService.createUserIfAbsent("13911110011", "10.0.0.1", null);

        assertServiceException(() -> userService.updateUserMobile(user.getId(), updateMobileReqVO("13911110011"),
                "10.0.0.9"), USER_MOBILE_USED);
        verifyNoInteractions(smsCodeApi);
    }

    @Test
    void getUserListByNickname_按昵称模糊匹配() {
        MemberUserDO user = userService.createUserIfAbsent("13911110012", "10.0.0.1", null);
        AppMemberUserUpdateReqVO reqVO = new AppMemberUserUpdateReqVO();
        reqVO.setNickname("会员测试昵称甲");
        userService.updateUser(user.getId(), reqVO);

        List<MemberUserDO> list = userService.getUserListByNickname("测试昵称");

        assertThat(list).extracting(MemberUserDO::getId).containsExactly(user.getId());
    }

    @Test
    void validateUserExists_不存在应抛USER_NOT_EXISTS() {
        assertServiceException(() -> userService.validateUserExists(-1L), USER_NOT_EXISTS);
    }

    private static AppMemberUserUpdateMobileReqVO updateMobileReqVO(String mobile) {
        AppMemberUserUpdateMobileReqVO reqVO = new AppMemberUserUpdateMobileReqVO();
        reqVO.setMobile(mobile);
        reqVO.setCode("new-code");
        reqVO.setOldCode("old-code");
        return reqVO;
    }
}
