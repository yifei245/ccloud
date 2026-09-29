package com.ccloud.module.system.service.notify;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.websocket.core.sender.WebSocketMessageSender;
import com.ccloud.module.system.dal.dataobject.notify.NotifyTemplateDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.NOTIFY_SEND_TEMPLATE_PARAM_MISS;
import static com.ccloud.module.system.enums.ErrorCodeConstants.NOTIFY_TEMPLATE_NOT_EXISTS;

@Slf4j
@Service
public class NotifySendServiceImpl implements NotifySendService {

    @Resource
    private NotifyTemplateService notifyTemplateService;
    @Resource
    private NotifyMessageService notifyMessageService;
    @Resource
    private WebSocketMessageSender webSocketMessageSender;

    @Override
    public Long sendSingleNotifyToAdmin(Long userId, String templateCode, Map<String, Object> templateParams) {
        return sendSingleNotify(userId, UserTypeEnum.ADMIN.getValue(), templateCode, templateParams);
    }

    @Override
    public Long sendSingleNotifyToMember(Long userId, String templateCode, Map<String, Object> templateParams) {
        return sendSingleNotify(userId, UserTypeEnum.MEMBER.getValue(), templateCode, templateParams);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendSingleNotify(Long userId, Integer userType, String templateCode, Map<String, Object> templateParams) {
        NotifyTemplateDO template = notifyTemplateService.getNotifyTemplateByCode(templateCode);
        if (template == null) {
            throw exception(NOTIFY_TEMPLATE_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(template.getStatus())) {
            log.info("[sendSingleNotify] 模板 {} 已关闭，不发送给用户 {}", templateCode, userId);
            return null;
        }
        Map<String, Object> params = templateParams == null ? Map.of() : templateParams;
        for (String key : template.getParams()) {
            if (params.get(key) == null) {
                throw exception(NOTIFY_SEND_TEMPLATE_PARAM_MISS, key);
            }
        }
        String content = notifyTemplateService.formatNotifyTemplateContent(template.getContent(), params);
        Long id = notifyMessageService.createNotifyMessage(userId, userType, template, content, params);

        Map<String, Object> push = new LinkedHashMap<>();
        push.put("id", id);
        push.put("templateNickname", template.getNickname());
        push.put("templateContent", content);
        push.put("templateType", template.getType());
        push.put("createTime", LocalDateTime.now());
        pushAfterCommit(() -> webSocketMessageSender.sendObject(userType, userId, WEBSOCKET_MESSAGE_TYPE, push));
        return id;
    }

    /**
     * 事务提交后再推送：若在提交前推送而事务随后回滚，用户会看到一条并不存在的消息
     */
    private static void pushAfterCommit(Runnable push) {
        Runnable safePush = () -> {
            try {
                push.run();
            } catch (RuntimeException e) {
                // 推送是尽力而为，失败不影响已落库的站内信
                log.warn("[pushAfterCommit] 站内信推送失败", e);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safePush.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safePush.run();
            }
        });
    }
}
