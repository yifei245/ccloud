package com.ccloud.module.system.dal.mysql.social;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.module.system.dal.dataobject.social.SocialUserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SocialUserMapper extends BaseMapperX<SocialUserDO> {

    default SocialUserDO selectByTypeAndCodeAndState(Integer type, String code, String state) {
        return selectOne(new com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX<SocialUserDO>()
                .eq(SocialUserDO::getType, type)
                .eq(SocialUserDO::getCode, code)
                .eq(SocialUserDO::getState, state));
    }

    default SocialUserDO selectByTypeAndOpenid(Integer type, String openid) {
        return selectOne(SocialUserDO::getType, type, SocialUserDO::getOpenid, openid);
    }
}
