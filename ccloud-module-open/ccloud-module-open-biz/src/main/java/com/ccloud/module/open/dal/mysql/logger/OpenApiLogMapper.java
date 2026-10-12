package com.ccloud.module.open.dal.mysql.logger;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.open.controller.admin.logger.vo.OpenApiLogPageReqVO;
import com.ccloud.module.open.dal.dataobject.logger.OpenApiLogDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface OpenApiLogMapper extends BaseMapperX<OpenApiLogDO> {

    default PageResult<OpenApiLogDO> selectPage(OpenApiLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OpenApiLogDO>()
                .eqIfPresent(OpenApiLogDO::getAppId, reqVO.getAppId())
                .eqIfPresent(OpenApiLogDO::getAppKey, reqVO.getAppKey())
                .eqIfPresent(OpenApiLogDO::getApiCode, reqVO.getApiCode())
                .eqIfPresent(OpenApiLogDO::getResultCode, reqVO.getResultCode())
                .betweenIfPresent(OpenApiLogDO::getBeginTime, reqVO.getBeginTime())
                .orderByDesc(OpenApiLogDO::getId));
    }

    /**
     * 物理删除：日志只增不改，逻辑删除只会让表越来越大
     */
    @Delete("DELETE FROM open_api_log WHERE create_time < #{before} LIMIT #{limit}")
    int deleteByCreateTimeLt(@Param("before") LocalDateTime before, @Param("limit") int limit);
}
