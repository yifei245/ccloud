package com.ccloud.module.system.service.notify;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import com.ccloud.module.system.controller.admin.notify.vo.message.NotifyMessagePageReqVO;
import com.ccloud.module.system.dal.dataobject.notify.NotifyMessageDO;
import com.ccloud.module.system.dal.dataobject.notify.NotifyTemplateDO;
import com.ccloud.module.system.dal.mysql.notify.NotifyMessageMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class NotifyMessageServiceImpl implements NotifyMessageService {

    @Resource
    private NotifyMessageMapper notifyMessageMapper;

    @Override
    public Long createNotifyMessage(Long userId, Integer userType, NotifyTemplateDO template, String content,
                                    Map<String, Object> templateParams) {
        NotifyMessageDO message = new NotifyMessageDO().setUserId(userId).setUserType(userType)
                .setTemplateId(template.getId()).setTemplateCode(template.getCode())
                .setTemplateNickname(template.getNickname()).setTemplateType(template.getType())
                .setTemplateContent(content).setTemplateParams(templateParams)
                .setReadStatus(false);
        notifyMessageMapper.insert(message);
        return message.getId();
    }

    @Override
    public NotifyMessageDO getNotifyMessage(Long id) {
        return notifyMessageMapper.selectById(id);
    }

    @Override
    public PageResult<NotifyMessageDO> getNotifyMessagePage(NotifyMessagePageReqVO pageReqVO) {
        return notifyMessageMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<NotifyMessageDO> getMyNotifyMessagePage(NotifyMessageMyPageReqVO pageReqVO, Long userId,
                                                              Integer userType) {
        return notifyMessageMapper.selectPage(pageReqVO, userId, userType);
    }

    @Override
    public List<NotifyMessageDO> getUnreadNotifyMessageList(Long userId, Integer userType, Integer size) {
        return notifyMessageMapper.selectUnreadListByUserId(userId, userType, size);
    }

    @Override
    public Long getUnreadNotifyMessageCount(Long userId, Integer userType) {
        return notifyMessageMapper.selectUnreadCountByUserId(userId, userType);
    }

    @Override
    public int updateNotifyMessageRead(Collection<Long> ids, Long userId, Integer userType) {
        // 空集合在查询条件里会被忽略，变成"全部已读"，必须挡住
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return notifyMessageMapper.updateListRead(ids, userId, userType);
    }

    @Override
    public int updateAllNotifyMessageRead(Long userId, Integer userType) {
        return notifyMessageMapper.updateListRead(null, userId, userType);
    }
}
