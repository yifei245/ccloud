package com.ccloud.module.open.api.notify;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.open.api.notify.dto.OpenNotifySendReqDTO;
import com.ccloud.module.open.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ApiConstants.NAME, contextId = "openNotifyApi")
@Tag(name = "RPC 服务 - 回调通知")
public interface OpenNotifyApi {

    String PREFIX = ApiConstants.PREFIX + "/notify";

    @PostMapping(PREFIX + "/send")
    @Operation(summary = "向应用发送事件回调", description = "写回调日志后异步发送，失败按退避重试；返回回调日志编号")
    CommonResult<Long> sendNotify(@Valid @RequestBody OpenNotifySendReqDTO reqDTO);
}
