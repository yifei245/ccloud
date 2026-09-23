package com.ccloud.module.system.api.sms;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ccloud.module.system.api.sms.dto.SmsSendSingleToUserReqDTO;
import jakarta.validation.Valid;

@FeignClient(name = ApiConstants.NAME, contextId = "systemSmsSendApi")
@Tag(name = "RPC 服务 - 短信发送")
public interface SmsSendApi {

    String PREFIX = ApiConstants.PREFIX + "/sms/send";

    @PostMapping(PREFIX + "/send-single-admin")
    @Operation(summary = "发送单条短信给管理员，返回短信日志编号")
    CommonResult<Long> sendSingleSmsToAdmin(@Valid @RequestBody SmsSendSingleToUserReqDTO reqDTO);

    @PostMapping(PREFIX + "/send-single-member")
    @Operation(summary = "发送单条短信给会员，返回短信日志编号")
    CommonResult<Long> sendSingleSmsToMember(@Valid @RequestBody SmsSendSingleToUserReqDTO reqDTO);
}
