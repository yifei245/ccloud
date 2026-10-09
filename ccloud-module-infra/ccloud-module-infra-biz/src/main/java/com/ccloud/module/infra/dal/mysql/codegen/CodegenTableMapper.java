package com.ccloud.module.infra.dal.mysql.codegen;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenTablePageReqVO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CodegenTableMapper extends BaseMapperX<CodegenTableDO> {

    default CodegenTableDO selectByDataSourceConfigIdAndTableName(Long dataSourceConfigId, String tableName) {
        return selectOne(CodegenTableDO::getDataSourceConfigId, dataSourceConfigId, CodegenTableDO::getTableName, tableName);
    }

    default List<CodegenTableDO> selectListByDataSourceConfigId(Long dataSourceConfigId) {
        return selectList(CodegenTableDO::getDataSourceConfigId, dataSourceConfigId);
    }

    default PageResult<CodegenTableDO> selectPage(CodegenTablePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CodegenTableDO>()
                .likeIfPresent(CodegenTableDO::getTableName, reqVO.getTableName())
                .likeIfPresent(CodegenTableDO::getTableComment, reqVO.getTableComment())
                .likeIfPresent(CodegenTableDO::getClassName, reqVO.getClassName())
                .betweenIfPresent(CodegenTableDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CodegenTableDO::getId));
    }
}
