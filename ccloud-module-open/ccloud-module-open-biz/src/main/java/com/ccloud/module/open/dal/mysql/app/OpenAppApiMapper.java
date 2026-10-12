package com.ccloud.module.open.dal.mysql.app;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.module.open.dal.dataobject.app.OpenAppApiDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OpenAppApiMapper extends BaseMapperX<OpenAppApiDO> {

    default List<OpenAppApiDO> selectListByAppId(Long appId) {
        return selectList(OpenAppApiDO::getAppId, appId);
    }

    default int deleteByAppId(Long appId) {
        return delete(OpenAppApiDO::getAppId, appId);
    }

    default int deleteByApiId(Long apiId) {
        return delete(OpenAppApiDO::getApiId, apiId);
    }
}
