package com.ccloud.module.member.dal.mysql.tag;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.member.controller.admin.tag.vo.MemberTagPageReqVO;
import com.ccloud.module.member.dal.dataobject.tag.MemberTagDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberTagMapper extends BaseMapperX<MemberTagDO> {

    default MemberTagDO selectByName(String name) {
        return selectOne(MemberTagDO::getName, name);
    }

    default PageResult<MemberTagDO> selectPage(MemberTagPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MemberTagDO>()
                .likeIfPresent(MemberTagDO::getName, reqVO.getName())
                .betweenIfPresent(MemberTagDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MemberTagDO::getId));
    }
}
