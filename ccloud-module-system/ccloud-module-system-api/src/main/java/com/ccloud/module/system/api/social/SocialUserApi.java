package com.ccloud.module.system.api.social;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ccloud.module.system.api.social.dto.SocialUserBindReqDTO;
import com.ccloud.module.system.api.social.dto.SocialUserRespDTO;
import com.ccloud.module.system.api.social.dto.SocialUserUnbindReqDTO;
import jakarta.validation.Valid;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 社交用户")
public interface SocialUserApi {

    String PREFIX = ApiConstants.PREFIX + "/social-user";

    @PostMapping(PREFIX + "/bind")
    @Operation(summary = "绑定社交账号，返回社交平台 openid")
    CommonResult<String> bindSocialUser(@Valid @RequestBody SocialUserBindReqDTO reqDTO);

    @DeleteMapping(PREFIX + "/unbind")
    @Operation(summary = "解绑社交账号")
    CommonResult<Boolean> unbindSocialUser(@Valid @RequestBody SocialUserUnbindReqDTO reqDTO);

    @GetMapping(PREFIX + "/get-by-user-id")
    @Operation(summary = "获得用户已绑定的社交账号")
    CommonResult<SocialUserRespDTO> getSocialUserByUserId(@RequestParam("userType") Integer userType,
                                                         @RequestParam("userId") Long userId,
                                                         @RequestParam("socialType") Integer socialType);

    /**
     * 用授权码换取社交用户。未绑定时返回的 userId 为空，调用方据此引导绑定或自动注册
     */
    @GetMapping(PREFIX + "/get-by-code")
    @Operation(summary = "用社交平台授权码获得社交用户")
    CommonResult<SocialUserRespDTO> getSocialUserByCode(@RequestParam("userType") Integer userType,
                                                       @RequestParam("socialType") Integer socialType,
                                                       @RequestParam("code") String code,
                                                       @RequestParam("state") String state);
}
