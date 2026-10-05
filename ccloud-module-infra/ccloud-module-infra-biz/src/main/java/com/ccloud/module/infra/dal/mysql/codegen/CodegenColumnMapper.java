package com.ccloud.module.infra.dal.mysql.codegen;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CodegenColumnMapper extends BaseMapperX<CodegenColumnDO> {

    default List<CodegenColumnDO> selectListByTableId(Long tableId) {
        return selectList(new LambdaQueryWrapper<CodegenColumnDO>()
                .eq(CodegenColumnDO::getTableId, tableId)
                .orderByAsc(CodegenColumnDO::getOrdinalPosition));
    }

    default void deleteListByTableId(Long tableId) {
        delete(CodegenColumnDO::getTableId, tableId);
    }
}
