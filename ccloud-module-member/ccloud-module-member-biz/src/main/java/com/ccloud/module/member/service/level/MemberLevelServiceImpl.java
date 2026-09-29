package com.ccloud.module.member.service.level;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.level.vo.MemberExperienceRecordPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelRecordPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelSaveReqVO;
import com.ccloud.module.member.dal.dataobject.level.MemberExperienceRecordDO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelDO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelRecordDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.level.MemberExperienceRecordMapper;
import com.ccloud.module.member.dal.mysql.level.MemberLevelMapper;
import com.ccloud.module.member.dal.mysql.level.MemberLevelRecordMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.ccloud.module.member.enums.ErrorCodeConstants.*;

@Service
@Validated
public class MemberLevelServiceImpl implements MemberLevelService {

    @Resource
    private MemberLevelMapper levelMapper;
    @Resource
    private MemberLevelRecordMapper levelRecordMapper;
    @Resource
    private MemberExperienceRecordMapper experienceRecordMapper;
    @Resource
    private MemberUserMapper userMapper;

    // ========== 等级配置 ==========

    @Override
    public Long createLevel(MemberLevelSaveReqVO reqVO) {
        validateLevelForCreateOrUpdate(null, reqVO.getLevel(), reqVO.getExperience());
        MemberLevelDO level = BeanUtils.toBean(reqVO, MemberLevelDO.class);
        levelMapper.insert(level);
        return level.getId();
    }

    @Override
    public void updateLevel(MemberLevelSaveReqVO reqVO) {
        validateLevelForCreateOrUpdate(reqVO.getId(), reqVO.getLevel(), reqVO.getExperience());
        levelMapper.updateById(BeanUtils.toBean(reqVO, MemberLevelDO.class));
    }

    @Override
    public void deleteLevel(Long id) {
        validateLevelExists(id);
        if (userMapper.selectCountByLevelId(id) > 0) {
            throw exception(LEVEL_HAS_USER);
        }
        levelMapper.deleteById(id);
    }

    /**
     * 与其他全部等级（含停用的）比较：序号不能重复，序号越大门槛经验必须越高。
     * 停用的等级随时可能重新启用，也要保持顺序
     */
    private void validateLevelForCreateOrUpdate(Long id, Integer level, Integer experience) {
        if (id != null) {
            validateLevelExists(id);
        }
        for (MemberLevelDO other : levelMapper.selectList()) {
            if (other.getId().equals(id)) {
                continue;
            }
            if (other.getLevel().equals(level)) {
                throw exception(LEVEL_VALUE_EXISTS);
            }
            boolean higher = level > other.getLevel();
            if (higher ? experience <= other.getExperience() : experience >= other.getExperience()) {
                throw exception(LEVEL_EXPERIENCE_ORDER_INVALID);
            }
        }
    }

    private MemberLevelDO validateLevelExists(Long id) {
        MemberLevelDO level = id == null ? null : levelMapper.selectById(id);
        if (level == null) {
            throw exception(LEVEL_NOT_EXISTS);
        }
        return level;
    }

    @Override
    public MemberLevelDO getLevel(Long id) {
        return levelMapper.selectById(id);
    }

    @Override
    public List<MemberLevelDO> getLevelList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return levelMapper.selectByIds(ids);
    }

    @Override
    public List<MemberLevelDO> getEnableLevelList() {
        return levelMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public PageResult<MemberLevelDO> getLevelPage(MemberLevelPageReqVO reqVO) {
        return levelMapper.selectPage(reqVO);
    }

    @Override
    public MemberLevelDO calculateLevel(int experience) {
        MemberLevelDO matched = null;
        // 按序号升序，门槛随序号递增，最后一个满足的就是最高等级
        for (MemberLevelDO level : getEnableLevelList()) {
            if (level.getExperience() <= experience) {
                matched = level;
            }
        }
        return matched;
    }

    // ========== 经验与等级变动 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addExperience(Long userId, Integer experience, Integer bizType, String bizId) {
        MemberExperienceBizTypeEnum type = validateBizType(experience, bizType, true);
        changeExperience(userId, experience, type, bizId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reduceExperience(Long userId, Integer experience, Integer bizType, String bizId) {
        MemberExperienceBizTypeEnum type = validateBizType(experience, bizType, false);
        changeExperience(userId, -experience, type, bizId);
    }

    private static MemberExperienceBizTypeEnum validateBizType(Integer experience, Integer bizType, boolean add) {
        if (experience == null || experience <= 0) {
            throw invalidParamException("经验必须大于 0");
        }
        MemberExperienceBizTypeEnum type = MemberExperienceBizTypeEnum.valueOf(bizType);
        if (type == null || type.isAdd() != add) {
            throw exception(EXPERIENCE_BIZ_TYPE_INVALID);
        }
        return type;
    }

    /**
     * 锁定会员行后查重、改经验、写记录、重算等级。经验不足时扣到 0，记录实际变动量
     */
    private void changeExperience(Long userId, int delta, MemberExperienceBizTypeEnum type, String bizId) {
        MemberUserDO user = lockUser(userId);
        if (experienceRecordMapper.selectByBiz(userId, type.getType(), bizId) != null) {
            return;
        }
        int before = ObjectUtil.defaultIfNull(user.getExperience(), 0);
        int total = Math.max(0, before + delta);
        insertExperienceRecord(userId, type, bizId, total - before, total);
        updateUserExperienceAndLevel(user, total, calculateLevel(total), "");
    }

    private void insertExperienceRecord(Long userId, MemberExperienceBizTypeEnum type, String bizId,
                                        int experience, int total) {
        // 管理员调整有增有减，业务类型的模板只适合增加，按正负号单独生成
        String description = type == MemberExperienceBizTypeEnum.ADMIN
                ? (experience >= 0 ? "管理员调整增加 " : "管理员调整扣减 ") + Math.abs(experience) + " 经验"
                : StrUtil.format(type.getDescription(), Math.abs(experience));
        experienceRecordMapper.insert(new MemberExperienceRecordDO().setUserId(userId)
                .setBizType(type.getType()).setBizId(bizId).setTitle(type.getName()).setDescription(description)
                .setExperience(experience).setTotalExperience(total));
    }

    /**
     * 写入经验与等级；等级变化时写等级变更记录。level 为 null（没有满足的启用等级）时清空会员等级
     */
    private void updateUserExperienceAndLevel(MemberUserDO user, int experience, MemberLevelDO level, String remark) {
        Long levelId = level == null ? null : level.getId();
        // 等级可能变为空，updateById 会忽略 null 字段，用 UpdateWrapper 显式写入
        userMapper.update(null, new LambdaUpdateWrapper<MemberUserDO>()
                .eq(MemberUserDO::getId, user.getId())
                .set(MemberUserDO::getExperience, experience)
                .set(MemberUserDO::getLevelId, levelId));
        if (Objects.equals(levelId, user.getLevelId())) {
            return;
        }
        MemberLevelDO oldLevel = user.getLevelId() == null ? null : levelMapper.selectById(user.getLevelId());
        boolean upgrade = level != null && (oldLevel == null || level.getLevel() > oldLevel.getLevel());
        levelRecordMapper.insert(new MemberLevelRecordDO().setUserId(user.getId())
                .setLevelId(level == null ? 0L : level.getId())
                .setLevel(level == null ? 0 : level.getLevel())
                .setDiscountPercent(level == null ? 100 : level.getDiscountPercent())
                .setExperience(level == null ? 0 : level.getExperience())
                .setUserExperience(experience).setRemark(remark)
                .setDescription((upgrade ? "升级为" : "降级为") + (level == null ? "无等级" : level.getName())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserLevel(Long userId, Long levelId, String reason) {
        MemberUserDO user = lockUser(userId);
        MemberLevelDO level = validateLevelExists(levelId);
        if (!CommonStatusEnum.isEnable(level.getStatus())) {
            throw exception(LEVEL_DISABLED);
        }
        // 已是该等级时不动经验：否则会把会员攒下的经验重置回门槛
        if (Objects.equals(user.getLevelId(), levelId)) {
            return;
        }
        int before = ObjectUtil.defaultIfNull(user.getExperience(), 0);
        if (level.getExperience() != before) {
            // 每次调整都应生效，业务编号随机生成
            insertExperienceRecord(userId, MemberExperienceBizTypeEnum.ADMIN, IdUtil.fastSimpleUUID(),
                    level.getExperience() - before, level.getExperience());
        }
        updateUserExperienceAndLevel(user, level.getExperience(), level, StrUtil.nullToEmpty(reason));
    }

    /**
     * 锁定会员行：同一会员的经验变动排队执行，查重与读写经验之间不会被其他请求插入
     */
    private MemberUserDO lockUser(Long userId) {
        MemberUserDO user = userId == null ? null : userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }

    @Override
    public PageResult<MemberLevelRecordDO> getLevelRecordPage(MemberLevelRecordPageReqVO reqVO) {
        return levelRecordMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<MemberExperienceRecordDO> getExperienceRecordPage(MemberExperienceRecordPageReqVO reqVO) {
        return experienceRecordMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<MemberExperienceRecordDO> getExperienceRecordPage(Long userId, PageParam pageParam) {
        return experienceRecordMapper.selectPage(userId, pageParam);
    }
}
