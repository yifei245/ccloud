package com.ccloud.module.member.service.group;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupPageReqVO;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupSaveReqVO;
import com.ccloud.module.member.dal.dataobject.group.MemberGroupDO;
import com.ccloud.module.member.dal.mysql.group.MemberGroupMapper;
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
public class MemberGroupServiceImpl implements MemberGroupService {

    @Resource
    private MemberGroupMapper groupMapper;
    @Resource
    private MemberUserMapper userMapper;

    @Override
    public Long createGroup(MemberGroupSaveReqVO reqVO) {
        validateNameUnique(null, reqVO.getName());
        MemberGroupDO group = BeanUtils.toBean(reqVO, MemberGroupDO.class);
        groupMapper.insert(group);
        return group.getId();
    }

    @Override
    public void updateGroup(MemberGroupSaveReqVO reqVO) {
        validateGroupExists(reqVO.getId());
        validateNameUnique(reqVO.getId(), reqVO.getName());
        groupMapper.updateById(BeanUtils.toBean(reqVO, MemberGroupDO.class));
    }

    @Override
    public void deleteGroup(Long id) {
        validateGroupExists(id);
        if (userMapper.selectCountByGroupId(id) > 0) {
            throw exception(GROUP_HAS_USER);
        }
        groupMapper.deleteById(id);
    }

    private MemberGroupDO validateGroupExists(Long id) {
        MemberGroupDO group = id == null ? null : groupMapper.selectById(id);
        if (group == null) {
            throw exception(GROUP_NOT_EXISTS);
        }
        return group;
    }

    private void validateNameUnique(Long id, String name) {
        MemberGroupDO group = groupMapper.selectByName(name);
        if (group != null && !group.getId().equals(id)) {
            throw exception(GROUP_NAME_EXISTS);
        }
    }

    @Override
    public MemberGroupDO getGroup(Long id) {
        return groupMapper.selectById(id);
    }

    @Override
    public List<MemberGroupDO> getGroupList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return groupMapper.selectByIds(ids);
    }

    @Override
    public List<MemberGroupDO> getEnableGroupList() {
        return groupMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public PageResult<MemberGroupDO> getGroupPage(MemberGroupPageReqVO reqVO) {
        return groupMapper.selectPage(reqVO);
    }

    @Override
    public void validateGroupEnabled(Long id) {
        MemberGroupDO group = validateGroupExists(id);
        if (!CommonStatusEnum.isEnable(group.getStatus())) {
            throw exception(GROUP_DISABLED);
        }
    }
}
