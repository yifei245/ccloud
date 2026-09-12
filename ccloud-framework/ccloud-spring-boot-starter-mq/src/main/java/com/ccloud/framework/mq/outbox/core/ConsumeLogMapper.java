package com.ccloud.framework.mq.outbox.core;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConsumeLogMapper extends BaseMapperX<ConsumeLogDO> {
}
