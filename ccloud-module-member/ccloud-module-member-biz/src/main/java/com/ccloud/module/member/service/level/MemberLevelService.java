package com.ccloud.module.member.service.level;

import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.member.controller.admin.level.vo.MemberExperienceRecordPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelRecordPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelSaveReqVO;
import com.ccloud.module.member.dal.dataobject.level.MemberExperienceRecordDO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelDO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelRecordDO;

import java.util.Collection;
import java.util.List;

/**
 * 会员等级：等级配置，以及经验变动与等级计算
 */
public interface MemberLevelService {

    // ========== 等级配置 ==========

    Long createLevel(MemberLevelSaveReqVO reqVO);

    void updateLevel(MemberLevelSaveReqVO reqVO);

    /**
     * @throws com.ccloud.framework.common.exception.ServiceException 仍有会员处于该等级时抛 LEVEL_HAS_USER
     */
    void deleteLevel(Long id);

    MemberLevelDO getLevel(Long id);

    List<MemberLevelDO> getLevelList(Collection<Long> ids);

    /**
     * @return 启用的等级，按序号升序
     */
    List<MemberLevelDO> getEnableLevelList();

    PageResult<MemberLevelDO> getLevelPage(MemberLevelPageReqVO reqVO);

    /**
     * 按经验计算等级：启用的等级中，门槛经验不超过 experience 的最高一个
     *
     * @return 没有满足的等级时返回 null
     */
    MemberLevelDO calculateLevel(int experience);

    // ========== 经验与等级变动 ==========

    /**
     * 增加经验并重算等级。同一「会员 + 业务类型 + 业务编号」重复调用只生效一次
     *
     * @param experience 大于 0
     * @param bizType    {@link com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum} 中「增加」类的类型
     */
    void addExperience(Long userId, Integer experience, Integer bizType, String bizId);

    /**
     * 扣减经验并重算等级，经验不足时扣到 0。同一业务编号重复调用只生效一次
     *
     * @param experience 大于 0
     * @param bizType    {@link com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum} 中「扣减」类的类型
     */
    void reduceExperience(Long userId, Integer experience, Integer bizType, String bizId);

    /**
     * 管理员修改会员等级：经验同步调到该等级的门槛。选择的就是当前等级时不做变更
     *
     * @param reason 修改原因，记入等级变更记录
     */
    void updateUserLevel(Long userId, Long levelId, String reason);

    PageResult<MemberLevelRecordDO> getLevelRecordPage(MemberLevelRecordPageReqVO reqVO);

    PageResult<MemberExperienceRecordDO> getExperienceRecordPage(MemberExperienceRecordPageReqVO reqVO);

    PageResult<MemberExperienceRecordDO> getExperienceRecordPage(Long userId, PageParam pageParam);
}
