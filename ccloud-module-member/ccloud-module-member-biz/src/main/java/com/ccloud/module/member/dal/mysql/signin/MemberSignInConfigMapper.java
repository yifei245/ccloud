package com.ccloud.module.member.dal.mysql.signin;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MemberSignInConfigMapper extends BaseMapperX<MemberSignInConfigDO> {

    default MemberSignInConfigDO selectByDay(Integer day) {
        return selectOne(MemberSignInConfigDO::getDay, day);
    }

    default List<MemberSignInConfigDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<MemberSignInConfigDO>()
                .eqIfPresent(MemberSignInConfigDO::getStatus, status)
                .orderByAsc(MemberSignInConfigDO::getDay));
    }
}
