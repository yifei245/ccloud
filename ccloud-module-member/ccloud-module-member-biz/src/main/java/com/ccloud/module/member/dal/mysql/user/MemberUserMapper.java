package com.ccloud.module.member.dal.mysql.user;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MemberUserMapper extends BaseMapperX<MemberUserDO> {

    default MemberUserDO selectByMobile(String mobile) {
        return selectOne(MemberUserDO::getMobile, mobile);
    }

    default List<MemberUserDO> selectListByNicknameLike(String nickname) {
        return selectList(new LambdaQueryWrapperX<MemberUserDO>().likeIfPresent(MemberUserDO::getNickname, nickname));
    }

    /**
     * 锁定会员行。积分、经验、等级、签到的写操作都在事务内先调用它，同一会员的变更排队执行
     */
    default MemberUserDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<MemberUserDO>().eq(MemberUserDO::getId, id).last("FOR UPDATE"));
    }

    default Long selectCountByLevelId(Long levelId) {
        return selectCount(MemberUserDO::getLevelId, levelId);
    }

    default Long selectCountByGroupId(Long groupId) {
        return selectCount(MemberUserDO::getGroupId, groupId);
    }

    /**
     * tag_ids 是逗号分隔的编号列表，用 FIND_IN_SET 精确匹配，避免 1 与 10 这类子串误判
     */
    default Long selectCountByTagId(Long tagId) {
        return selectCount(new LambdaQueryWrapperX<MemberUserDO>().apply("FIND_IN_SET({0}, tag_ids)", tagId));
    }
}
