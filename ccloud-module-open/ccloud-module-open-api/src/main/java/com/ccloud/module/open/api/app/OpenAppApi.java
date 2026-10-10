package com.ccloud.module.open.api.app;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.open.api.app.dto.OpenAppSnapshotRespDTO;
import com.ccloud.module.open.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME, contextId = "openAppApi")
@Tag(name = "RPC 服务 - 开放应用")
public interface OpenAppApi {

    String PREFIX = ApiConstants.PREFIX + "/app";

    @GetMapping(PREFIX + "/get-by-key")
    @Operation(summary = "按 appKey 获得应用快照", description = "供网关验签、限流、授权；含密钥原文。不存在返回 null；跨租户查询")
    CommonResult<OpenAppSnapshotRespDTO> getAppSnapshot(@RequestParam("appKey") String appKey);
}
