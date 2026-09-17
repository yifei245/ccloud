package com.ccloud.module.system.dal.mysql.permission;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.module.system.dal.dataobject.permission.MenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapperX<MenuDO> {

    default List<MenuDO> selectListByPermission(String permission) {
        return selectList(MenuDO::getPermission, permission);
    }
}
