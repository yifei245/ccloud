package com.ccloud.module.system.service.notify;

import java.util.Map;

/**
 * 站内信发送 Service：写入站内信表，事务提交后经 WebSocket 推送给在线用户（type 为 notify-message）。
 * 用户不在线时推送落空，上线后通过未读列表拉取
 */
public interface NotifySendService {

    String WEBSOCKET_MESSAGE_TYPE = "notify-message";

    /**
     * @return 站内信编号；模板关闭时不发送，返回 null
     */
    Long sendSingleNotifyToAdmin(Long userId, String templateCode, Map<String, Object> templateParams);

    /**
     * @return 站内信编号；模板关闭时不发送，返回 null
     */
    Long sendSingleNotifyToMember(Long userId, String templateCode, Map<String, Object> templateParams);

    Long sendSingleNotify(Long userId, Integer userType, String templateCode, Map<String, Object> templateParams);
}
