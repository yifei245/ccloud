package com.ccloud.module.infra.service.mq;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mq.outbox.core.DeadLetterDO;
import com.ccloud.module.infra.controller.admin.mq.vo.MessageDeadLetterPageReqVO;

/**
 * 消息死信：由平台管理员统一处理，查询覆盖全部租户
 */
public interface MessageDeadLetterService {

    PageResult<DeadLetterDO> getDeadLetterPage(MessageDeadLetterPageReqVO reqVO);

    /**
     * 标记为已人工处理（不重新投递）
     */
    void updateDeadLetterHandled(Long id);

    /**
     * 用原消息编号把消息发回原主题，成功后标记为已处理
     */
    void resendDeadLetter(Long id);
}
