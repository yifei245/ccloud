package com.ccloud.module.member.service.signin;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.member.controller.admin.signin.vo.MemberSignInConfigSaveReqVO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInConfigDO;
import com.ccloud.module.member.dal.mysql.signin.MemberSignInConfigMapper;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static com.ccloud.module.member.enums.ErrorCodeConstants.SIGN_IN_CONFIG_DAY_EXISTS;
import static com.ccloud.module.member.enums.ErrorCodeConstants.SIGN_IN_CONFIG_NOT_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;

@Import(MemberSignInConfigServiceImpl.class)
class MemberSignInConfigServiceImplTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberSignInConfigServiceImpl configService;
    @Resource
    private MemberSignInConfigMapper configMapper;

    @Test
    void createSignInConfig_天数重复() {
        configService.createSignInConfig(config(null, 1, 2, 1, CommonStatusEnum.ENABLE));
        assertServiceException(() -> configService.createSignInConfig(config(null, 1, 3, 1, CommonStatusEnum.ENABLE)),
                SIGN_IN_CONFIG_DAY_EXISTS);
    }

    @Test
    void updateSignInConfig_保持自身天数不算重复_改成他人天数报错() {
        Long first = configService.createSignInConfig(config(null, 1, 2, 1, CommonStatusEnum.ENABLE));
        configService.createSignInConfig(config(null, 2, 4, 2, CommonStatusEnum.ENABLE));
        configService.updateSignInConfig(config(first, 1, 5, 1, CommonStatusEnum.ENABLE));
        assertThat(configMapper.selectById(first).getPoint()).isEqualTo(5);
        assertServiceException(() -> configService.updateSignInConfig(config(first, 2, 5, 1, CommonStatusEnum.ENABLE)),
                SIGN_IN_CONFIG_DAY_EXISTS);
    }

    @Test
    void 修改或删除不存在的规则() {
        assertServiceException(() -> configService.updateSignInConfig(config(-1L, 1, 2, 1, CommonStatusEnum.ENABLE)),
                SIGN_IN_CONFIG_NOT_EXISTS);
        assertServiceException(() -> configService.deleteSignInConfig(-1L), SIGN_IN_CONFIG_NOT_EXISTS);
    }

    @Test
    void 同一天数的规则可反复删除后重建() {
        for (int i = 0; i < 3; i++) {
            Long id = configService.createSignInConfig(config(null, 1, 2, 1, CommonStatusEnum.ENABLE));
            configService.deleteSignInConfig(id);
        }
        assertThat(configService.getSignInConfigList(null)).isEmpty();
    }

    @Test
    void 物理删除仍受租户隔离() {
        Long id = configService.createSignInConfig(config(null, 1, 2, 1, CommonStatusEnum.ENABLE));
        assertThat(TenantUtils.execute(TENANT_ID + 1000, () -> configMapper.deleteByIdPhysically(id))).isZero();
        assertThat(configMapper.selectById(id)).isNotNull();
    }

    @Test
    void getSignInConfigList_按状态筛选_按天数升序() {
        configService.createSignInConfig(config(null, 7, 14, 7, CommonStatusEnum.ENABLE));
        configService.createSignInConfig(config(null, 1, 2, 1, CommonStatusEnum.ENABLE));
        configService.createSignInConfig(config(null, 3, 6, 3, CommonStatusEnum.DISABLE));

        assertThat(configService.getSignInConfigList(CommonStatusEnum.ENABLE.getStatus()))
                .extracting(MemberSignInConfigDO::getDay).containsExactly(1, 7);
        assertThat(configService.getSignInConfigList(null))
                .extracting(MemberSignInConfigDO::getDay).containsExactly(1, 3, 7);
    }

    static MemberSignInConfigSaveReqVO config(Long id, int day, int point, int experience, CommonStatusEnum status) {
        MemberSignInConfigSaveReqVO reqVO = new MemberSignInConfigSaveReqVO();
        reqVO.setId(id);
        reqVO.setDay(day);
        reqVO.setPoint(point);
        reqVO.setExperience(experience);
        reqVO.setStatus(status.getStatus());
        return reqVO;
    }
}
