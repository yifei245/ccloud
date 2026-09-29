package com.ccloud.module.member.service.level;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelSaveReqVO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelDO;

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
}
