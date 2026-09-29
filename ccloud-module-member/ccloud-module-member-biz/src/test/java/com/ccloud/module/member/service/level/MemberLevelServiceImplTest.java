package com.ccloud.module.member.service.level;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelSaveReqVO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.level.MemberLevelMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static com.ccloud.module.member.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import(MemberLevelServiceImpl.class)
class MemberLevelServiceImplTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberLevelServiceImpl levelService;
    @Resource
    private MemberLevelMapper levelMapper;
    @Resource
    private MemberUserMapper userMapper;

    // ========== 等级配置 ==========

    @Test
    void createLevel_成功() {
        Long id = levelService.createLevel(level("青铜", 1, 0));
        MemberLevelDO level = levelMapper.selectById(id);
        assertThat(level.getName()).isEqualTo("青铜");
        assertThat(level.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createLevel_等级序号重复() {
        levelService.createLevel(level("青铜", 1, 0));
        assertServiceException(() -> levelService.createLevel(level("白银", 1, 100)), LEVEL_VALUE_EXISTS);
    }

    @Test
    void createLevel_序号更高但门槛不高于低等级() {
        levelService.createLevel(level("白银", 2, 1000));
        assertServiceException(() -> levelService.createLevel(level("黄金", 3, 1000)), LEVEL_EXPERIENCE_ORDER_INVALID);
        assertServiceException(() -> levelService.createLevel(level("青铜", 1, 1000)), LEVEL_EXPERIENCE_ORDER_INVALID);
        assertServiceException(() -> levelService.createLevel(level("青铜", 1, 2000)), LEVEL_EXPERIENCE_ORDER_INVALID);
    }

    @Test
    void createLevel_停用的等级同样参与校验() {
        MemberLevelSaveReqVO disabled = level("白银", 2, 1000);
        disabled.setStatus(CommonStatusEnum.DISABLE.getStatus());
        levelService.createLevel(disabled);
        assertServiceException(() -> levelService.createLevel(level("白银二", 2, 1500)), LEVEL_VALUE_EXISTS);
    }

    @Test
    void updateLevel_与自身比较不算冲突() {
        Long id = levelService.createLevel(level("青铜", 1, 0));
        levelService.createLevel(level("白银", 2, 1000));
        MemberLevelSaveReqVO reqVO = level("青铜会员", 1, 10);
        reqVO.setId(id);
        levelService.updateLevel(reqVO);
        assertThat(levelMapper.selectById(id).getName()).isEqualTo("青铜会员");
        assertThat(levelMapper.selectById(id).getExperience()).isEqualTo(10);
    }

    @Test
    void updateLevel_不存在() {
        MemberLevelSaveReqVO reqVO = level("青铜", 1, 0);
        reqVO.setId(-1L);
        assertServiceException(() -> levelService.updateLevel(reqVO), LEVEL_NOT_EXISTS);
    }

    @Test
    void deleteLevel_有会员在用时拒绝() {
        Long id = levelService.createLevel(level("青铜", 1, 0));
        insertUser(id);
        assertServiceException(() -> levelService.deleteLevel(id), LEVEL_HAS_USER);
        assertThat(levelMapper.selectById(id)).isNotNull();
    }

    @Test
    void deleteLevel_成功与不存在() {
        Long id = levelService.createLevel(level("青铜", 1, 0));
        levelService.deleteLevel(id);
        assertThat(levelMapper.selectById(id)).isNull();
        assertServiceException(() -> levelService.deleteLevel(-1L), LEVEL_NOT_EXISTS);
    }

    @Test
    void getLevelPage_按名称与状态筛选_按序号升序() {
        levelService.createLevel(level("白银会员", 2, 1000));
        levelService.createLevel(level("青铜会员", 1, 0));
        MemberLevelSaveReqVO gold = level("黄金", 3, 5000);
        gold.setStatus(CommonStatusEnum.DISABLE.getStatus());
        levelService.createLevel(gold);

        MemberLevelPageReqVO reqVO = new MemberLevelPageReqVO();
        reqVO.setName("会员");
        assertThat(levelService.getLevelPage(reqVO).getList()).extracting(MemberLevelDO::getName)
                .containsExactly("青铜会员", "白银会员");
        reqVO.setName(null);
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        assertThat(levelService.getLevelPage(reqVO).getList()).extracting(MemberLevelDO::getName).containsExactly("黄金");
        assertThat(levelService.getEnableLevelList()).extracting(MemberLevelDO::getName)
                .containsExactly("青铜会员", "白银会员");
    }

    @Test
    void calculateLevel_取门槛不超过经验的最高启用等级_停用等级跳过() {
        Long bronze = levelService.createLevel(level("青铜", 1, 0));
        Long silver = levelService.createLevel(level("白银", 2, 1000));
        MemberLevelSaveReqVO gold = level("黄金", 3, 5000);
        gold.setStatus(CommonStatusEnum.DISABLE.getStatus());
        levelService.createLevel(gold);

        assertThat(levelService.calculateLevel(0).getId()).isEqualTo(bronze);
        assertThat(levelService.calculateLevel(999).getId()).isEqualTo(bronze);
        assertThat(levelService.calculateLevel(1000).getId()).isEqualTo(silver);
        assertThat(levelService.calculateLevel(9999).getId()).isEqualTo(silver);
    }

    @Test
    void calculateLevel_没有满足的等级返回null() {
        levelService.createLevel(level("白银", 2, 1000));
        assertThat(levelService.calculateLevel(10)).isNull();
    }

    static MemberLevelSaveReqVO level(String name, int level, int experience) {
        MemberLevelSaveReqVO reqVO = new MemberLevelSaveReqVO();
        reqVO.setName(name);
        reqVO.setLevel(level);
        reqVO.setExperience(experience);
        reqVO.setDiscountPercent(100);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }

    private void insertUser(Long levelId) {
        userMapper.insert(new MemberUserDO().setMobile("13900000001").setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("甲").setAvatar("").setPoint(0).setExperience(0)
                .setLevelId(levelId));
    }
}
