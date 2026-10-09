package com.ccloud.module.member.service.signin;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.module.member.controller.app.signin.vo.AppMemberSignInRecordSummaryRespVO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInConfigDO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInRecordDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.level.MemberExperienceRecordMapper;
import com.ccloud.module.member.dal.mysql.point.MemberPointRecordMapper;
import com.ccloud.module.member.dal.mysql.signin.MemberSignInConfigMapper;
import com.ccloud.module.member.dal.mysql.signin.MemberSignInRecordMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum;
import com.ccloud.module.member.enums.point.MemberPointBizTypeEnum;
import com.ccloud.module.member.service.level.MemberLevelServiceImpl;
import com.ccloud.module.member.service.point.MemberPointServiceImpl;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static com.ccloud.module.member.enums.ErrorCodeConstants.SIGN_IN_RECORD_TODAY_EXISTS;
import static com.ccloud.module.member.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import static com.ccloud.module.member.service.signin.MemberSignInConfigServiceImplTest.config;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 签到规则：第 1 天 (2 积分, 1 经验)、第 3 天 (6, 3)、第 7 天 (14, 7)
 */
@Import({MemberSignInRecordServiceImpl.class, MemberSignInConfigServiceImpl.class,
        MemberPointServiceImpl.class, MemberLevelServiceImpl.class})
class MemberSignInRecordServiceImplTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberSignInRecordServiceImpl signInService;
    @Resource
    private MemberSignInConfigServiceImpl configService;
    @Resource
    private MemberSignInConfigMapper configMapper;
    @Resource
    private MemberSignInRecordMapper recordMapper;
    @Resource
    private MemberPointRecordMapper pointRecordMapper;
    @Resource
    private MemberExperienceRecordMapper experienceRecordMapper;
    @Resource
    private MemberUserMapper userMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        configService.createSignInConfig(config(null, 1, 2, 1, CommonStatusEnum.ENABLE));
        configService.createSignInConfig(config(null, 3, 6, 3, CommonStatusEnum.ENABLE));
        configService.createSignInConfig(config(null, 7, 14, 7, CommonStatusEnum.ENABLE));
        MemberUserDO user = new MemberUserDO().setMobile("13900000001").setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("甲").setAvatar("").setPoint(0).setExperience(0);
        userMapper.insert(user);
        userId = user.getId();
    }

    @Test
    void 首次签到为第1天_发放积分与经验_以记录编号为业务编号() {
        MemberSignInRecordDO record = signInService.createSignRecord(userId);

        assertThat(record.getDay()).isEqualTo(1);
        assertThat(record.getPoint()).isEqualTo(2);
        assertThat(record.getExperience()).isEqualTo(1);
        assertThat(recordMapper.selectById(record.getId()).getUserId()).isEqualTo(userId);
        MemberUserDO user = userMapper.selectById(userId);
        assertThat(user.getPoint()).isEqualTo(2);
        assertThat(user.getExperience()).isEqualTo(1);
        assertThat(pointRecordMapper.selectByBiz(userId, MemberPointBizTypeEnum.SIGN.getType(),
                record.getId().toString())).isNotNull();
        assertThat(experienceRecordMapper.selectByBiz(userId, MemberExperienceBizTypeEnum.SIGN_IN.getType(),
                record.getId().toString())).isNotNull();
    }

    @Test
    void 今天已签到时拒绝() {
        signInService.createSignRecord(userId);
        assertServiceException(() -> signInService.createSignRecord(userId), SIGN_IN_RECORD_TODAY_EXISTS);
        assertThat(recordMapper.selectCount()).isEqualTo(1);
    }

    @Test
    void 今天早些时候签过_即使在昨天之后也算今天已签() {
        insertRecord(3, LocalDateTime.now().toLocalDate().atStartOfDay());
        assertServiceException(() -> signInService.createSignRecord(userId), SIGN_IN_RECORD_TODAY_EXISTS);
    }

    @Test
    void 昨天签过则连续天数加1_规则不连续时取不超过当前天数的最大规则() {
        insertRecord(1, LocalDateTime.now().minusDays(1));
        MemberSignInRecordDO record = signInService.createSignRecord(userId);
        assertThat(record.getDay()).isEqualTo(2);
        assertThat(record.getPoint()).isEqualTo(2);   // 没有第 2 天规则，按第 1 天
        assertThat(record.getExperience()).isEqualTo(1);
    }

    @Test
    void 正好命中配置天数() {
        insertRecord(2, LocalDateTime.now().minusDays(1));
        assertThat(signInService.createSignRecord(userId).getPoint()).isEqualTo(6);
    }

    @Test
    void 超过最大配置天数后一直按最大天数发放_天数照常累加() {
        insertRecord(9, LocalDateTime.now().minusDays(1));
        MemberSignInRecordDO record = signInService.createSignRecord(userId);
        assertThat(record.getDay()).isEqualTo(10);
        assertThat(record.getPoint()).isEqualTo(14);
        assertThat(record.getExperience()).isEqualTo(7);
    }

    @Test
    void 断签后从第1天开始() {
        insertRecord(5, LocalDateTime.now().minusDays(2));
        assertThat(signInService.createSignRecord(userId).getDay()).isEqualTo(1);
    }

    @Test
    void 停用的规则不参与计算() {
        MemberSignInConfigDO day3 = configMapper.selectOne(MemberSignInConfigDO::getDay, 3);
        configMapper.updateById(new MemberSignInConfigDO().setId(day3.getId()).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        insertRecord(2, LocalDateTime.now().minusDays(1));
        assertThat(signInService.createSignRecord(userId).getPoint()).isEqualTo(2);
    }

    @Test
    void 没有可用规则时照常签到_奖励为0且不写积分经验记录() {
        configMapper.selectList().forEach(config -> configMapper.updateById(new MemberSignInConfigDO()
                .setId(config.getId()).setStatus(CommonStatusEnum.DISABLE.getStatus())));

        MemberSignInRecordDO record = signInService.createSignRecord(userId);

        assertThat(record.getDay()).isEqualTo(1);
        assertThat(record.getPoint()).isZero();
        assertThat(record.getExperience()).isZero();
        assertThat(pointRecordMapper.selectCount()).isZero();
        assertThat(experienceRecordMapper.selectCount()).isZero();
    }

    @Test
    void 会员不存在() {
        assertServiceException(() -> signInService.createSignRecord(-1L), USER_NOT_EXISTS);
    }

    @Test
    void 签到概况() {
        insertRecord(1, LocalDateTime.now().minusDays(3));
        insertRecord(1, LocalDateTime.now().minusDays(1));
        AppMemberSignInRecordSummaryRespVO before = signInService.getSignInRecordSummary(userId);
        assertThat(before.getTodaySignIn()).isFalse();
        assertThat(before.getContinuousDay()).isEqualTo(1);
        assertThat(before.getTotalDay()).isEqualTo(2);

        signInService.createSignRecord(userId);

        AppMemberSignInRecordSummaryRespVO after = signInService.getSignInRecordSummary(userId);
        assertThat(after.getTodaySignIn()).isTrue();
        assertThat(after.getContinuousDay()).isEqualTo(2);
        assertThat(after.getTotalDay()).isEqualTo(3);
    }

    @Test
    void 签到概况_最后一次在前天之前时连续天数为0_从未签到时全为0() {
        AppMemberSignInRecordSummaryRespVO empty = signInService.getSignInRecordSummary(userId);
        assertThat(empty.getTodaySignIn()).isFalse();
        assertThat(empty.getContinuousDay()).isZero();
        assertThat(empty.getTotalDay()).isZero();

        insertRecord(4, LocalDateTime.now().minusDays(2));
        assertThat(signInService.getSignInRecordSummary(userId).getContinuousDay()).isZero();
    }

    @Test
    void getSignInRecordPage_用户端只看本人_按时间倒序() {
        insertRecord(1, LocalDateTime.now().minusDays(2));
        signInService.createSignRecord(userId);
        MemberUserDO other = new MemberUserDO().setMobile("13900000002").setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("乙").setAvatar("").setPoint(0).setExperience(0);
        userMapper.insert(other);
        signInService.createSignRecord(other.getId());

        assertThat(signInService.getSignInRecordPage(userId, new PageParam()).getList())
                .extracting(MemberSignInRecordDO::getDay).containsExactly(1, 1)
                .hasSize(2);
        assertThat(signInService.getSignInRecordPage(userId, new PageParam()).getList().getFirst().getCreateTime())
                .isAfter(LocalDateTime.now().minusMinutes(1));
    }

    private void insertRecord(int day, LocalDateTime createTime) {
        MemberSignInRecordDO record = new MemberSignInRecordDO().setUserId(userId).setDay(day).setPoint(0).setExperience(0);
        record.setCreateTime(createTime);
        recordMapper.insert(record);
    }
}
