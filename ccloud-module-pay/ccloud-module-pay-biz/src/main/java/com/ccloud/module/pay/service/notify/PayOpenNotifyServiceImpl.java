package com.ccloud.module.pay.service.notify;

import com.ccloud.module.open.api.notify.OpenNotifyApi;
import com.ccloud.module.open.api.notify.dto.OpenNotifySendReqDTO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.service.app.PayAppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PayOpenNotifyServiceImpl implements PayOpenNotifyService {

    @Resource
    private PayAppService appService;
    @Resource
    private OpenNotifyApi openNotifyApi;

    /**
     * 以消息编号作为事件编号：消费事务提交失败导致消息重投时，开放平台按事件编号去重，第三方只收到一次。
     * 应用查询忽略租户：转发所需的只是应用编号对应的开放应用绑定，这一查询不应依赖消费线程当前的租户上下文
     */
    @Override
    public void forward(String messageId, String event, Long appId, Map<String, Object> data) {
        PayAppDO app = appService.getAppIgnoreTenant(appId);
        if (app == null || app.getOpenAppId() == null) {
            return;
        }
        // OpenNotifySendReqDTO 的 setter 不支持链式调用
        OpenNotifySendReqDTO req = new OpenNotifySendReqDTO();
        req.setAppId(app.getOpenAppId());
        req.setEventId(messageId);
        req.setEvent(event);
        req.setData(data);
        openNotifyApi.sendNotify(req).getCheckedData();
    }
}
