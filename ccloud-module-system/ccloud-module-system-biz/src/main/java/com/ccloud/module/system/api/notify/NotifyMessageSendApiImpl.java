package com.ccloud.module.system.api.notify;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import com.ccloud.module.system.service.notify.NotifySendService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 站内信 RPC 实现：工作流、商城等模块给用户发站内信依赖它
 */
@RestController
@Validated
public class NotifyMessageSendApiImpl implements NotifyMessageSendApi {

    @Resource
    private NotifySendService notifySendService;

    @Override
    public CommonResult<Long> sendSingleMessageToAdmin(NotifySendSingleToUserReqDTO reqDTO) {
        return success(notifySendService.sendSingleNotifyToAdmin(reqDTO.getUserId(), reqDTO.getTemplateCode(),
                reqDTO.getTemplateParams()));
    }

    @Override
    public CommonResult<Long> sendSingleMessageToMember(NotifySendSingleToUserReqDTO reqDTO) {
        return success(notifySendService.sendSingleNotifyToMember(reqDTO.getUserId(), reqDTO.getTemplateCode(),
                reqDTO.getTemplateParams()));
    }
}
