package com.ccloud.module.member.dal.mysql.config;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.member.dal.dataobject.config.MemberConfigDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberConfigMapper extends BaseMapperX<MemberConfigDO> {

    /**
     * 当前租户的配置。多租户拦截器自动加上租户条件
     */
    default MemberConfigDO selectCurrent() {
        return selectOne(new LambdaQueryWrapperX<MemberConfigDO>().orderByAsc(MemberConfigDO::getId).last("LIMIT 1"));
    }
}
