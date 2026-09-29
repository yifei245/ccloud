package com.ccloud.module.member.dal.mysql.signin;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInConfigDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberSignInConfigMapper extends BaseMapperX<MemberSignInConfigDO> {

    default MemberSignInConfigDO selectByDay(Integer day) {
        return selectOne(MemberSignInConfigDO::getDay, day);
    }

    /**
     * 物理删除。唯一索引 uk_day 含 deleted 列，逻辑删除后 deleted 恒为 1，同一天数的规则第二次删除会与
     * 第一次删除的记录冲突；规则删除后没有保留价值，签到记录也不引用规则。多租户拦截器会追加租户条件
     */
    @Delete("DELETE FROM member_sign_in_config WHERE id = #{id}")
    int deleteByIdPhysically(@Param("id") Long id);

    default List<MemberSignInConfigDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<MemberSignInConfigDO>()
                .eqIfPresent(MemberSignInConfigDO::getStatus, status)
                .orderByAsc(MemberSignInConfigDO::getDay));
    }
}
