package com.ccloud.module.system.dal.mysql.sms;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.system.dal.dataobject.sms.SmsCodeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SmsCodeMapper extends BaseMapperX<SmsCodeDO> {

    /**
     * @return 该手机号该场景最近一条验证码
     */
    default SmsCodeDO selectLastByMobile(String mobile, Integer scene) {
        return selectOne(new LambdaQueryWrapperX<SmsCodeDO>()
                .eq(SmsCodeDO::getMobile, mobile)
                .eq(SmsCodeDO::getScene, scene)
                .orderByDesc(SmsCodeDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 仅当未使用时标记为已使用，并发使用同一验证码只有一个能成功
     *
     * @return 更新行数
     */
    default int updateToUsed(Long id, SmsCodeDO update) {
        return update(update, new LambdaQueryWrapperX<SmsCodeDO>()
                .eq(SmsCodeDO::getId, id)
                .eq(SmsCodeDO::getUsed, false));
    }
}
