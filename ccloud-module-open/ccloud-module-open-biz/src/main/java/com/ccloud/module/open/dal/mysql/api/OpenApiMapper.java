package com.ccloud.module.open.dal.mysql.api;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.open.controller.admin.api.vo.OpenApiPageReqVO;
import com.ccloud.module.open.dal.dataobject.api.OpenApiDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OpenApiMapper extends BaseMapperX<OpenApiDO> {

    default PageResult<OpenApiDO> selectPage(OpenApiPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OpenApiDO>()
                .likeIfPresent(OpenApiDO::getName, reqVO.getName())
                .likeIfPresent(OpenApiDO::getCode, reqVO.getCode())
                .likeIfPresent(OpenApiDO::getPath, reqVO.getPath())
                .eqIfPresent(OpenApiDO::getStatus, reqVO.getStatus())
                .orderByDesc(OpenApiDO::getId));
    }

    default OpenApiDO selectByCode(String code) {
        return selectOne(OpenApiDO::getCode, code);
    }

    default OpenApiDO selectByMethodAndPath(String method, String path) {
        return selectOne(OpenApiDO::getMethod, method, OpenApiDO::getPath, path);
    }

    default List<OpenApiDO> selectListByStatus(Integer status) {
        return selectList(OpenApiDO::getStatus, status);
    }
}
