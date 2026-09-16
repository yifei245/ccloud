package com.ccloud.module.system.dal.mysql.permission;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.module.system.dal.dataobject.permission.RoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapperX<RoleDO> {

    default RoleDO selectByName(String name) {
        return selectOne(RoleDO::getName, name);
    }

    default RoleDO selectByCode(String code) {
        return selectOne(RoleDO::getCode, code);
    }

    default List<RoleDO> selectListByStatus(Integer status) {
        return selectList(RoleDO::getStatus, status);
    }
}
