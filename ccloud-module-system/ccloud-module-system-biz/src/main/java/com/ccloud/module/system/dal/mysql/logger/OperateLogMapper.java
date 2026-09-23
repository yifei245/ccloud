package com.ccloud.module.system.dal.mysql.logger;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.system.controller.admin.logger.vo.OperateLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.logger.OperateLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperateLogMapper extends BaseMapperX<OperateLogDO> {

    default PageResult<OperateLogDO> selectPage(OperateLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OperateLogDO>()
                .eqIfPresent(OperateLogDO::getUserId, reqVO.getUserId())
                .likeIfPresent(OperateLogDO::getType, reqVO.getType())
                .likeIfPresent(OperateLogDO::getSubType, reqVO.getSubType())
                .eqIfPresent(OperateLogDO::getBizId, reqVO.getBizId())
                .likeIfPresent(OperateLogDO::getAction, reqVO.getAction())
                .eqIfPresent(OperateLogDO::getSuccess, reqVO.getSuccess())
                .betweenIfPresent(OperateLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(OperateLogDO::getId));
    }
}
