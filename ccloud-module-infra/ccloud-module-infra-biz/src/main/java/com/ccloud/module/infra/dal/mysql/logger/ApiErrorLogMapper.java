package com.ccloud.module.infra.dal.mysql.logger;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiErrorLogPageReqVO;
import com.ccloud.module.infra.dal.dataobject.logger.ApiErrorLogDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ApiErrorLogMapper extends BaseMapperX<ApiErrorLogDO> {

    default PageResult<ApiErrorLogDO> selectPage(ApiErrorLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ApiErrorLogDO>()
                .eqIfPresent(ApiErrorLogDO::getUserId, reqVO.getUserId())
                .eqIfPresent(ApiErrorLogDO::getUserType, reqVO.getUserType())
                .eqIfPresent(ApiErrorLogDO::getApplicationName, reqVO.getApplicationName())
                .likeIfPresent(ApiErrorLogDO::getRequestUrl, reqVO.getRequestUrl())
                .betweenIfPresent(ApiErrorLogDO::getExceptionTime, reqVO.getExceptionTime())
                .eqIfPresent(ApiErrorLogDO::getProcessStatus, reqVO.getProcessStatus())
                .orderByDesc(ApiErrorLogDO::getId));
    }

    /**
     * 物理删除：日志只增不改，逻辑删除只会让表越来越大
     */
    @Delete("DELETE FROM infra_api_error_log WHERE create_time < #{before} LIMIT #{limit}")
    int deleteByCreateTimeLt(@Param("before") LocalDateTime before, @Param("limit") int limit);
}
