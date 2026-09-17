package com.ccloud.module.system.dal.mysql.dict;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypePageReqVO;
import com.ccloud.module.system.dal.dataobject.dict.DictTypeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictTypeMapper extends BaseMapperX<DictTypeDO> {

    default PageResult<DictTypeDO> selectPage(DictTypePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DictTypeDO>()
                .likeIfPresent(DictTypeDO::getName, reqVO.getName())
                .likeIfPresent(DictTypeDO::getType, reqVO.getType())
                .eqIfPresent(DictTypeDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(DictTypeDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DictTypeDO::getId));
    }

    default DictTypeDO selectByType(String type) {
        return selectOne(DictTypeDO::getType, type);
    }

    default DictTypeDO selectByName(String name) {
        return selectOne(DictTypeDO::getName, name);
    }
}
