package com.ccloud.module.member.service.level;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelSaveReqVO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelDO;
import com.ccloud.module.member.dal.mysql.level.MemberLevelMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.member.enums.ErrorCodeConstants.*;

@Service
@Validated
public class MemberLevelServiceImpl implements MemberLevelService {

    @Resource
    private MemberLevelMapper levelMapper;
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
}
