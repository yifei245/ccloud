package com.ccloud.module.member.service.tag;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.tag.vo.MemberTagPageReqVO;
import com.ccloud.module.member.controller.admin.tag.vo.MemberTagSaveReqVO;
import com.ccloud.module.member.dal.dataobject.tag.MemberTagDO;
import com.ccloud.module.member.dal.mysql.tag.MemberTagMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.member.enums.ErrorCodeConstants.*;

@Service
@Validated
public class MemberTagServiceImpl implements MemberTagService {

    @Resource
    private MemberTagMapper tagMapper;
    @Resource
    private MemberUserMapper userMapper;

    @Override
    public Long createTag(MemberTagSaveReqVO reqVO) {
        validateNameUnique(null, reqVO.getName());
        MemberTagDO tag = BeanUtils.toBean(reqVO, MemberTagDO.class);
        tagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    public void updateTag(MemberTagSaveReqVO reqVO) {
        validateTagExists(reqVO.getId());
        validateNameUnique(reqVO.getId(), reqVO.getName());
        tagMapper.updateById(BeanUtils.toBean(reqVO, MemberTagDO.class));
    }

    @Override
    public void deleteTag(Long id) {
        validateTagExists(id);
        if (userMapper.selectCountByTagId(id) > 0) {
            throw exception(TAG_HAS_USER);
        }
        tagMapper.deleteById(id);
    }

    private void validateTagExists(Long id) {
        if (id == null || tagMapper.selectById(id) == null) {
            throw exception(TAG_NOT_EXISTS);
        }
    }

    private void validateNameUnique(Long id, String name) {
        MemberTagDO tag = tagMapper.selectByName(name);
        if (tag != null && !tag.getId().equals(id)) {
            throw exception(TAG_NAME_EXISTS);
        }
    }

    @Override
    public MemberTagDO getTag(Long id) {
        return tagMapper.selectById(id);
    }

    @Override
    public List<MemberTagDO> getTagList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return tagMapper.selectByIds(ids);
    }

    @Override
    public List<MemberTagDO> getTagList() {
        return tagMapper.selectList();
    }

    @Override
    public PageResult<MemberTagDO> getTagPage(MemberTagPageReqVO reqVO) {
        return tagMapper.selectPage(reqVO);
    }

    @Override
    public void validateTagList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        Set<Long> existing = CollectionUtils.convertSet(tagMapper.selectByIds(ids), MemberTagDO::getId);
        for (Long id : ids) {
            if (!existing.contains(id)) {
                throw exception(TAG_NOT_EXISTS);
            }
        }
    }
}
