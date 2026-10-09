package com.ccloud.module.member.service.signin;

import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.member.controller.admin.signin.vo.MemberSignInRecordPageReqVO;
import com.ccloud.module.member.controller.app.signin.vo.AppMemberSignInRecordSummaryRespVO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInRecordDO;

/**
 * 签到。「今天」「昨天」按应用默认时区的自然日计算
 */
public interface MemberSignInRecordService {

    /**
     * 签到并发放奖励：昨天签过则连续天数加 1，否则从 1 开始；
     * 奖励取启用规则中天数不超过连续天数的最大一条
     *
     * @throws com.ccloud.framework.common.exception.ServiceException 今天已签到时抛 SIGN_IN_RECORD_TODAY_EXISTS
     */
    MemberSignInRecordDO createSignRecord(Long userId);

    AppMemberSignInRecordSummaryRespVO getSignInRecordSummary(Long userId);

    PageResult<MemberSignInRecordDO> getSignInRecordPage(MemberSignInRecordPageReqVO reqVO);

    PageResult<MemberSignInRecordDO> getSignInRecordPage(Long userId, PageParam pageParam);
}
