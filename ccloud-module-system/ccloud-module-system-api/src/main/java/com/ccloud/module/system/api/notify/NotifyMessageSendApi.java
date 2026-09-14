package com.ccloud.module.system.api.notify;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ccloud.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import jakarta.validation.Valid;

/**
 * 站内信发送 RPC。发送后会经 WebSocket 实时推送给在线的接收人
 */
@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 站内信发送")
public interface NotifyMessageSendApi {

    String PREFIX = ApiConstants.PREFIX + "/notify/send";

    @PostMapping(PREFIX + "/send-single-admin")
    @Operation(summary = "发送单条站内信给管理员，返回站内信编号")
    CommonResult<Long> sendSingleMessageToAdmin(@Valid @RequestBody NotifySendSingleToUserReqDTO reqDTO);

    @PostMapping(PREFIX + "/send-single-member")
    @Operation(summary = "发送单条站内信给会员，返回站内信编号")
    CommonResult<Long> sendSingleMessageToMember(@Valid @RequestBody NotifySendSingleToUserReqDTO reqDTO);
}
