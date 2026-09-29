package com.ccloud.module.member.service.signin;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.member.controller.admin.signin.vo.MemberSignInRecordPageReqVO;
import com.ccloud.module.member.controller.app.signin.vo.AppMemberSignInRecordSummaryRespVO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInConfigDO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInRecordDO;
import com.ccloud.module.member.dal.mysql.signin.MemberSignInRecordMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum;
import com.ccloud.module.member.enums.point.MemberPointBizTypeEnum;
import com.ccloud.module.member.service.level.MemberLevelService;
import com.ccloud.module.member.service.point.MemberPointService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.member.enums.ErrorCodeConstants.SIGN_IN_RECORD_TODAY_EXISTS;
import static com.ccloud.module.member.enums.ErrorCodeConstants.USER_NOT_EXISTS;

@Service
@Validated
public class MemberSignInRecordServiceImpl implements MemberSignInRecordService {

    @Resource
    private MemberSignInRecordMapper recordMapper;
    @Resource
    private MemberUserMapper userMapper;
    @Resource
    private MemberSignInConfigService configService;
    @Resource
    private MemberPointService pointService;
    @Resource
    private MemberLevelService levelService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberSignInRecordDO createSignRecord(Long userId) {
        // 锁会员行：同一会员的并发签到排队执行，后到的请求能看到先到的请求写入的记录
        if (userId == null || userMapper.selectByIdForUpdate(userId) == null) {
            throw exception(USER_NOT_EXISTS);
        }
        LocalDate today = LocalDate.now();
        MemberSignInRecordDO last = recordMapper.selectLastByUserId(userId);
        LocalDate lastDate = last == null ? null : last.getCreateTime().toLocalDate();
        if (today.equals(lastDate)) {
            throw exception(SIGN_IN_RECORD_TODAY_EXISTS);
        }
        int day = today.minusDays(1).equals(lastDate) ? last.getDay() + 1 : 1;
        MemberSignInConfigDO rule = findRule(day);
        MemberSignInRecordDO record = new MemberSignInRecordDO().setUserId(userId).setDay(day)
                .setPoint(rule == null ? 0 : rule.getPoint())
                .setExperience(rule == null ? 0 : rule.getExperience());
        recordMapper.insert(record);
        // 以签到记录编号作为业务编号，奖励与签到记录一一对应
        String bizId = record.getId().toString();
        if (record.getPoint() > 0) {
            pointService.addPoint(userId, record.getPoint(), MemberPointBizTypeEnum.SIGN.getType(), bizId);
        }
        if (record.getExperience() > 0) {
            levelService.addExperience(userId, record.getExperience(), MemberExperienceBizTypeEnum.SIGN_IN.getType(), bizId);
        }
        return record;
    }

    /**
     * 启用规则中天数不超过连续天数的最大一条：超过最大配置天数后一直按最大天数发放，规则不连续时取前一条
     */
    private MemberSignInConfigDO findRule(int day) {
        MemberSignInConfigDO matched = null;
        for (MemberSignInConfigDO config : configService.getSignInConfigList(CommonStatusEnum.ENABLE.getStatus())) {
            if (config.getDay() <= day) {
                matched = config;
            }
        }
        return matched;
    }

    @Override
    public AppMemberSignInRecordSummaryRespVO getSignInRecordSummary(Long userId) {
        AppMemberSignInRecordSummaryRespVO summary = new AppMemberSignInRecordSummaryRespVO();
        summary.setTotalDay(recordMapper.selectCount(MemberSignInRecordDO::getUserId, userId).intValue());
        MemberSignInRecordDO last = recordMapper.selectLastByUserId(userId);
        LocalDate lastDate = last == null ? null : last.getCreateTime().toLocalDate();
        LocalDate today = LocalDate.now();
        summary.setTodaySignIn(today.equals(lastDate));
        // 最后一次在今天或昨天，连续签到还在延续；更早则已断签
        boolean continuing = today.equals(lastDate) || today.minusDays(1).equals(lastDate);
        summary.setContinuousDay(continuing ? last.getDay() : 0);
        return summary;
    }

    @Override
    public PageResult<MemberSignInRecordDO> getSignInRecordPage(MemberSignInRecordPageReqVO reqVO) {
        return recordMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<MemberSignInRecordDO> getSignInRecordPage(Long userId, PageParam pageParam) {
        return recordMapper.selectPage(userId, pageParam);
    }
}
