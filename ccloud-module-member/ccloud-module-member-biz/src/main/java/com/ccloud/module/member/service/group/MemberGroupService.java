package com.ccloud.module.member.service.group;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupPageReqVO;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupSaveReqVO;
import com.ccloud.module.member.dal.dataobject.group.MemberGroupDO;

import java.util.Collection;
import java.util.List;

public interface MemberGroupService {

    Long createGroup(MemberGroupSaveReqVO reqVO);

    void updateGroup(MemberGroupSaveReqVO reqVO);

    /**
     * @throws com.ccloud.framework.common.exception.ServiceException 仍有会员属于该分组时抛 GROUP_HAS_USER
     */
    void deleteGroup(Long id);

    MemberGroupDO getGroup(Long id);

    List<MemberGroupDO> getGroupList(Collection<Long> ids);

    List<MemberGroupDO> getEnableGroupList();

    PageResult<MemberGroupDO> getGroupPage(MemberGroupPageReqVO reqVO);

    /**
     * 分配给会员前校验分组存在且启用
     *
     * @throws com.ccloud.framework.common.exception.ServiceException 不存在抛 GROUP_NOT_EXISTS，停用抛 GROUP_DISABLED
     */
    void validateGroupEnabled(Long id);
}
