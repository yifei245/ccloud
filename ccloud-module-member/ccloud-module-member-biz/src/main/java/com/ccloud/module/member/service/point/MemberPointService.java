package com.ccloud.module.member.service.point;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.member.controller.admin.point.vo.MemberPointRecordPageReqVO;
import com.ccloud.module.member.controller.app.point.vo.AppMemberPointRecordPageReqVO;
import com.ccloud.module.member.dal.dataobject.point.MemberPointRecordDO;

/**
 * 会员积分。积分的一切变动都经过本服务：锁定会员行后查重、改余额、写记录
 */
public interface MemberPointService {

    /**
     * 增加积分。同一「会员 + 业务类型 + 业务编号」重复调用只生效一次
     *
     * @param point   大于 0
     * @param bizType {@link com.ccloud.module.member.enums.point.MemberPointBizTypeEnum} 中「增加」类的类型
     */
    void addPoint(Long userId, Integer point, Integer bizType, String bizId);

    /**
     * 扣减积分。同一业务编号重复调用只生效一次
     *
     * @param point   大于 0
     * @param bizType {@link com.ccloud.module.member.enums.point.MemberPointBizTypeEnum} 中「扣减」类的类型
     * @throws com.ccloud.framework.common.exception.ServiceException 余额不足时抛 USER_POINT_NOT_ENOUGH
     */
    void reducePoint(Long userId, Integer point, Integer bizType, String bizId);

    /**
     * 管理员调整积分，每次调整都生效
     *
     * @param point 非 0，正数增加、负数扣减；调整后不能小于 0
     */
    void updateUserPointByAdmin(Long userId, Integer point);

    PageResult<MemberPointRecordDO> getPointRecordPage(MemberPointRecordPageReqVO reqVO);

    PageResult<MemberPointRecordDO> getPointRecordPage(Long userId, AppMemberPointRecordPageReqVO reqVO);
}
