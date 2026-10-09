package com.ccloud.module.member.service.level;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.member.dal.dataobject.level.MemberExperienceRecordDO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelDO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelRecordDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.level.MemberExperienceRecordMapper;
import com.ccloud.module.member.dal.mysql.level.MemberLevelMapper;
import com.ccloud.module.member.dal.mysql.level.MemberLevelRecordMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.ccloud.module.member.enums.ErrorCodeConstants.*;
import static com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum.ORDER_GIVE;
import static com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum.ORDER_GIVE_CANCEL;
import static com.ccloud.module.member.service.level.MemberLevelServiceImplTest.level;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 经验变动与等级重算。等级：青铜（1, 0）、白银（2, 1000）、黄金（3, 5000）
 */
@Import(MemberLevelServiceImpl.class)
class MemberLevelExperienceTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberLevelServiceImpl levelService;
    @Resource
    private MemberLevelMapper levelMapper;
    @Resource
    private MemberLevelRecordMapper levelRecordMapper;
    @Resource
    private MemberExperienceRecordMapper experienceRecordMapper;
    @Resource
    private MemberUserMapper userMapper;

    private Long bronzeId;
    private Long silverId;
    private Long goldId;
    private Long userId;

    @BeforeEach
    void setUp() {
        bronzeId = levelService.createLevel(level("青铜", 1, 0));
        silverId = levelService.createLevel(level("白银", 2, 1000));
        goldId = levelService.createLevel(level("黄金", 3, 5000));
        MemberUserDO user = new MemberUserDO().setMobile("13900000001").setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("甲").setAvatar("").setPoint(0).setExperience(0)
                .setLevelId(bronzeId);
        userMapper.insert(user);
        userId = user.getId();
    }

    @Test
    void addExperience_记录变动并升级_写等级变更记录() {
        levelService.addExperience(userId, 1200, ORDER_GIVE.getType(), "order-1");

        MemberUserDO user = userMapper.selectById(userId);
        assertThat(user.getExperience()).isEqualTo(1200);
        assertThat(user.getLevelId()).isEqualTo(silverId);
        MemberExperienceRecordDO record = experienceRecordMapper.selectByBiz(userId, ORDER_GIVE.getType(), "order-1");
        assertThat(record.getExperience()).isEqualTo(1200);
        assertThat(record.getTotalExperience()).isEqualTo(1200);
        assertThat(record.getTitle()).isEqualTo("下单奖励");
        assertThat(record.getDescription()).isEqualTo("下单获得 1200 经验");
        MemberLevelRecordDO levelRecord = levelRecordMapper.selectList().getFirst();
        assertThat(levelRecord.getUserId()).isEqualTo(userId);
        assertThat(levelRecord.getLevelId()).isEqualTo(silverId);
        assertThat(levelRecord.getLevel()).isEqualTo(2);
        assertThat(levelRecord.getExperience()).isEqualTo(1000);
        assertThat(levelRecord.getUserExperience()).isEqualTo(1200);
        assertThat(levelRecord.getDescription()).isEqualTo("升级为白银");
    }

    @Test
    void addExperience_跨多级直接升到最高满足的等级() {
        levelService.addExperience(userId, 6000, ORDER_GIVE.getType(), "order-1");
        assertThat(userMapper.selectById(userId).getLevelId()).isEqualTo(goldId);
        assertThat(levelRecordMapper.selectCount()).isEqualTo(1);
    }

    @Test
    void addExperience_同一业务编号只生效一次() {
        levelService.addExperience(userId, 100, ORDER_GIVE.getType(), "order-1");
        levelService.addExperience(userId, 100, ORDER_GIVE.getType(), "order-1");
        assertThat(userMapper.selectById(userId).getExperience()).isEqualTo(100);
        assertThat(experienceRecordMapper.selectCount()).isEqualTo(1);
    }

    @Test
    void addExperience_不同业务类型的相同编号互不影响() {
        levelService.addExperience(userId, 100, ORDER_GIVE.getType(), "order-1");
        levelService.reduceExperience(userId, 30, ORDER_GIVE_CANCEL.getType(), "order-1");
        assertThat(userMapper.selectById(userId).getExperience()).isEqualTo(70);
    }

    @Test
    void addExperience_业务类型方向不符或数量不合法() {
        assertServiceException(() -> levelService.addExperience(userId, 10, ORDER_GIVE_CANCEL.getType(), "x"),
                EXPERIENCE_BIZ_TYPE_INVALID);
        assertServiceException(() -> levelService.reduceExperience(userId, 10, ORDER_GIVE.getType(), "x"),
                EXPERIENCE_BIZ_TYPE_INVALID);
        assertServiceException(() -> levelService.addExperience(userId, 10, 999, "x"), EXPERIENCE_BIZ_TYPE_INVALID);
        assertServiceException(() -> levelService.addExperience(userId, 0, ORDER_GIVE.getType(), "x"), BAD_REQUEST);
        assertServiceException(() -> levelService.reduceExperience(userId, -5, ORDER_GIVE_CANCEL.getType(), "x"),
                BAD_REQUEST);
        assertThat(experienceRecordMapper.selectCount()).isZero();
    }

    @Test
    void reduceExperience_不足时扣到0并降级_记录实际扣减量() {
        levelService.addExperience(userId, 1200, ORDER_GIVE.getType(), "order-1");
        levelService.reduceExperience(userId, 5000, ORDER_GIVE_CANCEL.getType(), "order-1");

        MemberUserDO user = userMapper.selectById(userId);
        assertThat(user.getExperience()).isZero();
        assertThat(user.getLevelId()).isEqualTo(bronzeId);
        MemberExperienceRecordDO record = experienceRecordMapper.selectByBiz(userId, ORDER_GIVE_CANCEL.getType(), "order-1");
        assertThat(record.getExperience()).isEqualTo(-1200);
        assertThat(record.getTotalExperience()).isZero();
        assertThat(record.getDescription()).isEqualTo("退单扣除 1200 经验");
        assertThat(levelRecordMapper.selectList()).extracting(MemberLevelRecordDO::getDescription)
                .containsExactly("升级为白银", "降级为青铜");
    }

    @Test
    void addExperience_等级未变时不写等级记录() {
        levelService.addExperience(userId, 10, ORDER_GIVE.getType(), "order-1");
        assertThat(levelRecordMapper.selectCount()).isZero();
        assertThat(userMapper.selectById(userId).getLevelId()).isEqualTo(bronzeId);
    }

    @Test
    void addExperience_所在等级停用后按启用等级重算() {
        levelService.addExperience(userId, 1200, ORDER_GIVE.getType(), "order-1");
        levelMapper.updateById(new MemberLevelDO().setId(silverId).setStatus(CommonStatusEnum.DISABLE.getStatus()));

        levelService.addExperience(userId, 10, ORDER_GIVE.getType(), "order-2");

        assertThat(userMapper.selectById(userId).getLevelId()).isEqualTo(bronzeId);
    }

    @Test
    void addExperience_没有满足的启用等级时清空会员等级() {
        levelMapper.updateById(new MemberLevelDO().setId(bronzeId).setStatus(CommonStatusEnum.DISABLE.getStatus()));

        levelService.addExperience(userId, 10, ORDER_GIVE.getType(), "order-1");

        assertThat(userMapper.selectById(userId).getLevelId()).isNull();
        MemberLevelRecordDO record = levelRecordMapper.selectList().getFirst();
        assertThat(record.getLevelId()).isZero();
        assertThat(record.getDescription()).isEqualTo("降级为无等级");
    }

    @Test
    void addExperience_会员不存在() {
        assertServiceException(() -> levelService.addExperience(-1L, 10, ORDER_GIVE.getType(), "x"), USER_NOT_EXISTS);
    }

    @Test
    void updateUserLevel_经验调到门槛_写经验与等级记录() {
        levelService.updateUserLevel(userId, goldId, "活动赠送");

        MemberUserDO user = userMapper.selectById(userId);
        assertThat(user.getLevelId()).isEqualTo(goldId);
        assertThat(user.getExperience()).isEqualTo(5000);
        MemberExperienceRecordDO exp = experienceRecordMapper.selectList().getFirst();
        assertThat(exp.getBizType()).isEqualTo(MemberExperienceBizTypeEnum.ADMIN.getType());
        assertThat(exp.getBizId()).isNotBlank();
        assertThat(exp.getExperience()).isEqualTo(5000);
        assertThat(exp.getTotalExperience()).isEqualTo(5000);
        assertThat(exp.getDescription()).isEqualTo("管理员调整增加 5000 经验");
        MemberLevelRecordDO record = levelRecordMapper.selectList().getFirst();
        assertThat(record.getRemark()).isEqualTo("活动赠送");
        assertThat(record.getDescription()).isEqualTo("升级为黄金");
    }

    @Test
    void updateUserLevel_降级时扣减经验_每次调整都生效() {
        levelService.updateUserLevel(userId, goldId, "活动赠送");
        levelService.updateUserLevel(userId, bronzeId, "活动结束");
        levelService.updateUserLevel(userId, goldId, "再次赠送");

        assertThat(experienceRecordMapper.selectList()).extracting(MemberExperienceRecordDO::getDescription)
                .containsExactly("管理员调整增加 5000 经验", "管理员调整扣减 5000 经验", "管理员调整增加 5000 经验");
        assertThat(levelRecordMapper.selectList()).extracting(MemberLevelRecordDO::getDescription)
                .containsExactly("升级为黄金", "降级为青铜", "升级为黄金");
        assertThat(userMapper.selectById(userId).getExperience()).isEqualTo(5000);
    }

    @Test
    void updateUserLevel_选择当前等级时不做变更() {
        levelService.addExperience(userId, 300, ORDER_GIVE.getType(), "order-1");
        levelService.updateUserLevel(userId, bronzeId, "无变化");
        assertThat(userMapper.selectById(userId).getExperience()).isEqualTo(300);
        assertThat(levelRecordMapper.selectCount()).isZero();
        assertThat(experienceRecordMapper.selectCount()).isEqualTo(1);
    }

    @Test
    void updateUserLevel_等级停用或不存在() {
        levelMapper.updateById(new MemberLevelDO().setId(goldId).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        assertServiceException(() -> levelService.updateUserLevel(userId, goldId, "x"), LEVEL_DISABLED);
        assertServiceException(() -> levelService.updateUserLevel(userId, -1L, "x"), LEVEL_NOT_EXISTS);
        assertServiceException(() -> levelService.updateUserLevel(-1L, silverId, "x"), USER_NOT_EXISTS);
    }
}
