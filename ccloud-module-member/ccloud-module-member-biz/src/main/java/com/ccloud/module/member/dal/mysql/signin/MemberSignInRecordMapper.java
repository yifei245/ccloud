package com.ccloud.module.member.dal.mysql.signin;

import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.member.controller.admin.signin.vo.MemberSignInRecordPageReqVO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberSignInRecordMapper extends BaseMapperX<MemberSignInRecordDO> {

    default MemberSignInRecordDO selectLastByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<MemberSignInRecordDO>()
                .eq(MemberSignInRecordDO::getUserId, userId)
                .orderByDesc(MemberSignInRecordDO::getCreateTime)
                .orderByDesc(MemberSignInRecordDO::getId)
                .last("LIMIT 1"));
    }

    default PageResult<MemberSignInRecordDO> selectPage(MemberSignInRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MemberSignInRecordDO>()
                .eqIfPresent(MemberSignInRecordDO::getUserId, reqVO.getUserId())
                .eqIfPresent(MemberSignInRecordDO::getDay, reqVO.getDay())
                .betweenIfPresent(MemberSignInRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MemberSignInRecordDO::getId));
    }

    default PageResult<MemberSignInRecordDO> selectPage(Long userId, PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<MemberSignInRecordDO>()
                .eq(MemberSignInRecordDO::getUserId, userId)
                .orderByDesc(MemberSignInRecordDO::getCreateTime)
                .orderByDesc(MemberSignInRecordDO::getId));
    }
}
