package com.ccloud.module.member.dal.mysql.address;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.member.dal.dataobject.address.MemberAddressDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MemberAddressMapper extends BaseMapperX<MemberAddressDO> {

    default MemberAddressDO selectByIdAndUserId(Long id, Long userId) {
        return selectOne(MemberAddressDO::getId, id, MemberAddressDO::getUserId, userId);
    }

    default MemberAddressDO selectDefaultByUserId(Long userId) {
        return selectOne(MemberAddressDO::getUserId, userId, MemberAddressDO::getDefaultStatus, true);
    }

    /**
     * 默认地址在前，其余按编号倒序
     */
    default List<MemberAddressDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<MemberAddressDO>()
                .eq(MemberAddressDO::getUserId, userId)
                .orderByDesc(MemberAddressDO::getDefaultStatus)
                .orderByDesc(MemberAddressDO::getId));
    }

    default void updateDefaultFalseByUserId(Long userId) {
        update(new MemberAddressDO().setDefaultStatus(false), new LambdaQueryWrapperX<MemberAddressDO>()
                .eq(MemberAddressDO::getUserId, userId)
                .eq(MemberAddressDO::getDefaultStatus, true));
    }
}
