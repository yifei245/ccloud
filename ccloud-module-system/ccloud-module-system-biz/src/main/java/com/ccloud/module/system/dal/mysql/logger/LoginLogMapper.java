package com.ccloud.module.system.dal.mysql.logger;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.system.controller.admin.logger.vo.LoginLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.logger.LoginLogDO;
import com.ccloud.module.system.enums.logger.LoginResultEnum;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapperX<LoginLogDO> {

    default PageResult<LoginLogDO> selectPage(LoginLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LoginLogDO>()
                .likeIfPresent(LoginLogDO::getUserIp, reqVO.getUserIp())
                .likeIfPresent(LoginLogDO::getUsername, reqVO.getUsername())
                .eqIfPresent(LoginLogDO::getLogType, reqVO.getLogType())
                .eq(Boolean.TRUE.equals(reqVO.getStatus()), LoginLogDO::getResult, LoginResultEnum.SUCCESS.getResult())
                .betweenIfPresent(LoginLogDO::getCreateTime, reqVO.getCreateTime())
                .neIfPresent(LoginLogDO::getResult,
                        Boolean.FALSE.equals(reqVO.getStatus()) ? LoginResultEnum.SUCCESS.getResult() : null)
                .orderByDesc(LoginLogDO::getId));
    }
}
