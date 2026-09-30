package com.ccloud.module.member.service.tag;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.member.controller.admin.tag.vo.MemberTagPageReqVO;
import com.ccloud.module.member.controller.admin.tag.vo.MemberTagSaveReqVO;
import com.ccloud.module.member.dal.dataobject.tag.MemberTagDO;

import java.util.Collection;
import java.util.List;

public interface MemberTagService {

    Long createTag(MemberTagSaveReqVO reqVO);

    void updateTag(MemberTagSaveReqVO reqVO);

    /**
     * @throws com.ccloud.framework.common.exception.ServiceException 仍有会员使用该标签时抛 TAG_HAS_USER
     */
    void deleteTag(Long id);

    MemberTagDO getTag(Long id);

    List<MemberTagDO> getTagList(Collection<Long> ids);

    List<MemberTagDO> getTagList();

    PageResult<MemberTagDO> getTagPage(MemberTagPageReqVO reqVO);

    /**
     * 校验标签全部存在，空集合直接通过
     *
     * @throws com.ccloud.framework.common.exception.ServiceException 任一不存在时抛 TAG_NOT_EXISTS
     */
    void validateTagList(Collection<Long> ids);
}
