package com.ccloud.module.system.api.social;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ccloud.module.system.api.social.dto.SocialWxPhoneNumberInfoRespDTO;

@FeignClient(name = ApiConstants.NAME, contextId = "systemSocialClientApi")
@Tag(name = "RPC 服务 - 社交应用")
public interface SocialClientApi {

    String PREFIX = ApiConstants.PREFIX + "/social-client";

    @GetMapping(PREFIX + "/get-authorize-url")
    @Operation(summary = "获得社交平台的授权跳转地址")
    CommonResult<String> getAuthorizeUrl(@RequestParam("socialType") Integer socialType,
                                         @RequestParam("userType") Integer userType,
                                         @RequestParam("redirectUri") String redirectUri);

    @GetMapping(PREFIX + "/get-wxa-phone-number-info")
    @Operation(summary = "用微信小程序 phoneCode 换取手机号，小程序一键登录使用")
    CommonResult<SocialWxPhoneNumberInfoRespDTO> getWxMaPhoneNumberInfo(@RequestParam("userType") Integer userType,
                                                                        @RequestParam("phoneCode") String phoneCode);
}
