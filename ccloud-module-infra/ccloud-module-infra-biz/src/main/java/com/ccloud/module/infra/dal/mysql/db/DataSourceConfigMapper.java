package com.ccloud.module.infra.dal.mysql.db;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.module.infra.dal.dataobject.db.DataSourceConfigDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataSourceConfigMapper extends BaseMapperX<DataSourceConfigDO> {
}
