package com.ccloud.module.infra.dal.mysql.codegen;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CodegenTableMapper extends BaseMapperX<CodegenTableDO> {
}
