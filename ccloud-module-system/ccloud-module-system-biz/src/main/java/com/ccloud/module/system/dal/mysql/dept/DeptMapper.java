package com.ccloud.module.system.dal.mysql.dept;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.module.system.dal.dataobject.dept.DeptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DeptMapper extends BaseMapperX<DeptDO> {

    default List<DeptDO> selectListByParentId(Collection<Long> parentIds) {
        return selectList(DeptDO::getParentId, parentIds);
    }
}
