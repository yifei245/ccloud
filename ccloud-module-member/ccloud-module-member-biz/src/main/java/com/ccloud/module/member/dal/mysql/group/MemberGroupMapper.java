package com.ccloud.module.member.dal.mysql.group;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupPageReqVO;
import com.ccloud.module.member.dal.dataobject.group.MemberGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MemberGroupMapper extends BaseMapperX<MemberGroupDO> {

    default MemberGroupDO selectByName(String name) {
        return selectOne(MemberGroupDO::getName, name);
    }

    default List<MemberGroupDO> selectListByStatus(Integer status) {
        return selectList(MemberGroupDO::getStatus, status);
    }

    default PageResult<MemberGroupDO> selectPage(MemberGroupPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MemberGroupDO>()
                .likeIfPresent(MemberGroupDO::getName, reqVO.getName())
                .eqIfPresent(MemberGroupDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MemberGroupDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MemberGroupDO::getId));
    }
}
