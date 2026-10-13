package com.ccloud.module.pay.dal.mysql.channel;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PayChannelMapper extends BaseMapperX<PayChannelDO> {

    default PayChannelDO selectByAppIdAndCode(Long appId, String code) {
        return selectOne(PayChannelDO::getAppId, appId, PayChannelDO::getCode, code);
    }

    default List<PayChannelDO> selectListByAppId(Long appId) {
        return selectList(new LambdaQueryWrapperX<PayChannelDO>()
                .eq(PayChannelDO::getAppId, appId)
                .orderByAsc(PayChannelDO::getId));
    }

    default List<PayChannelDO> selectListByAppIdAndStatus(Long appId, Integer status) {
        return selectList(new LambdaQueryWrapperX<PayChannelDO>()
                .eq(PayChannelDO::getAppId, appId)
                .eq(PayChannelDO::getStatus, status)
                .orderByAsc(PayChannelDO::getId));
    }
}
