package com.ccloud.module.pay.api.wallet;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.pay.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME, contextId = "payWalletApi")
@Tag(name = "RPC 服务 - 钱包")
public interface PayWalletApi {

    String PREFIX = ApiConstants.PREFIX + "/wallet";

    @GetMapping(PREFIX + "/get-balance")
    @Operation(summary = "获得钱包余额", description = "钱包不存在时按 0 返回，单位分")
    CommonResult<Integer> getWalletBalance(@RequestParam("userId") Long userId,
                                           @RequestParam("userType") Integer userType);
}
