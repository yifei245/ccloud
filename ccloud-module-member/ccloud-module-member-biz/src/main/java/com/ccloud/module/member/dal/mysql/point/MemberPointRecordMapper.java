package com.ccloud.module.member.dal.mysql.point;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.member.controller.admin.point.vo.MemberPointRecordPageReqVO;
import com.ccloud.module.member.controller.app.point.vo.AppMemberPointRecordPageReqVO;
import com.ccloud.module.member.dal.dataobject.point.MemberPointRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberPointRecordMapper extends BaseMapperX<MemberPointRecordDO> {

    default MemberPointRecordDO selectByBiz(Long userId, Integer bizType, String bizId) {
        return selectOne(new LambdaQueryWrapperX<MemberPointRecordDO>()
                .eq(MemberPointRecordDO::getUserId, userId)
                .eq(MemberPointRecordDO::getBizType, bizType)
                .eq(MemberPointRecordDO::getBizId, bizId));
    }

    default PageResult<MemberPointRecordDO> selectPage(MemberPointRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MemberPointRecordDO>()
                .eqIfPresent(MemberPointRecordDO::getUserId, reqVO.getUserId())
                .eqIfPresent(MemberPointRecordDO::getBizType, reqVO.getBizType())
                .likeIfPresent(MemberPointRecordDO::getTitle, reqVO.getTitle())
                .betweenIfPresent(MemberPointRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MemberPointRecordDO::getId));
    }

    /**
     * addStatus 为 true 只看收入，false 只看支出，null 看全部
     */
    default PageResult<MemberPointRecordDO> selectPage(Long userId, AppMemberPointRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<MemberPointRecordDO> query = new LambdaQueryWrapperX<MemberPointRecordDO>()
                .eq(MemberPointRecordDO::getUserId, userId);
        if (Boolean.TRUE.equals(reqVO.getAddStatus())) {
            query.gt(MemberPointRecordDO::getPoint, 0);
        } else if (Boolean.FALSE.equals(reqVO.getAddStatus())) {
            query.lt(MemberPointRecordDO::getPoint, 0);
        }
        return selectPage(reqVO, query.orderByDesc(MemberPointRecordDO::getId));
    }
}
