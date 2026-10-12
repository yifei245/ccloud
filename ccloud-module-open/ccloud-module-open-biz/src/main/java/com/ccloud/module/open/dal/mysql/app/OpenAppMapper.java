package com.ccloud.module.open.dal.mysql.app;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.open.controller.admin.app.vo.OpenAppPageReqVO;
import com.ccloud.module.open.dal.dataobject.app.OpenAppDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OpenAppMapper extends BaseMapperX<OpenAppDO> {

    default PageResult<OpenAppDO> selectPage(OpenAppPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OpenAppDO>()
                .likeIfPresent(OpenAppDO::getName, reqVO.getName())
                .eqIfPresent(OpenAppDO::getAppKey, reqVO.getAppKey())
                .eqIfPresent(OpenAppDO::getStatus, reqVO.getStatus())
                .orderByDesc(OpenAppDO::getId));
    }

    default OpenAppDO selectByAppKey(String appKey) {
        return selectOne(OpenAppDO::getAppKey, appKey);
    }
}
