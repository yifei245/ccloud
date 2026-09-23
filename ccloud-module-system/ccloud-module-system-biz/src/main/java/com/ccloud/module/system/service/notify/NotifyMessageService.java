package com.ccloud.module.system.service.notify;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import com.ccloud.module.system.controller.admin.notify.vo.message.NotifyMessagePageReqVO;
import com.ccloud.module.system.dal.dataobject.notify.NotifyMessageDO;
import com.ccloud.module.system.dal.dataobject.notify.NotifyTemplateDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 站内信 Service。"我的站内信"系列方法均以 userId + userType 限定范围
 */
public interface NotifyMessageService {

    Long createNotifyMessage(Long userId, Integer userType, NotifyTemplateDO template, String content,
                             Map<String, Object> templateParams);

    NotifyMessageDO getNotifyMessage(Long id);

    /**
     * 管理后台查询全部站内信
     */
    PageResult<NotifyMessageDO> getNotifyMessagePage(NotifyMessagePageReqVO pageReqVO);

    PageResult<NotifyMessageDO> getMyNotifyMessagePage(NotifyMessageMyPageReqVO pageReqVO, Long userId, Integer userType);

    /**
     * @param size 最多返回条数
     * @return 最新的未读消息
     */
    List<NotifyMessageDO> getUnreadNotifyMessageList(Long userId, Integer userType, Integer size);

    Long getUnreadNotifyMessageCount(Long userId, Integer userType);

    /**
     * 只会更新属于该用户的消息
     *
     * @return 实际标记为已读的条数
     */
    int updateNotifyMessageRead(Collection<Long> ids, Long userId, Integer userType);

    /**
     * @return 实际标记为已读的条数
     */
    int updateAllNotifyMessageRead(Long userId, Integer userType);
}
