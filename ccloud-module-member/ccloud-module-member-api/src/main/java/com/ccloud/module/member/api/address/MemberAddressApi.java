package com.ccloud.module.member.api.address;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.member.api.address.dto.MemberAddressRespDTO;
import com.ccloud.module.member.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME, contextId = "memberMemberAddressApi")
@Tag(name = "RPC 服务 - 会员收件地址")
public interface MemberAddressApi {

    String PREFIX = ApiConstants.PREFIX + "/address";

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "获得用户的收件地址，地址不属于该用户时返回 null")
    CommonResult<MemberAddressRespDTO> getAddress(@RequestParam("id") Long id, @RequestParam("userId") Long userId);

    @GetMapping(PREFIX + "/get-default")
    @Operation(summary = "获得用户的默认收件地址，未设置返回 null")
    CommonResult<MemberAddressRespDTO> getDefaultAddress(@RequestParam("userId") Long userId);
}
