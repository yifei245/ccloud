package com.ccloud.module.system.api.sms;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ccloud.module.system.api.sms.dto.SmsCodeSendReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeUseReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeValidateReqDTO;
import jakarta.validation.Valid;

/**
 * 短信验证码 RPC。验证码的发送频率、每日上限、有效期、一次性使用全部在 system 侧控制，
 * 调用方不应自行实现任何一项——分散实现必然出现有的入口不限频的漏洞。
 */
@FeignClient(name = ApiConstants.NAME, contextId = "systemSmsCodeApi")
@Tag(name = "RPC 服务 - 短信验证码")
public interface SmsCodeApi {

    String PREFIX = ApiConstants.PREFIX + "/sms-code";

    @PostMapping(PREFIX + "/send")
    @Operation(summary = "发送短信验证码")
    CommonResult<Boolean> sendSmsCode(@Valid @RequestBody SmsCodeSendReqDTO reqDTO);

    @PutMapping(PREFIX + "/use")
    @Operation(summary = "使用短信验证码，校验通过后立即标记已使用")
    CommonResult<Boolean> useSmsCode(@Valid @RequestBody SmsCodeUseReqDTO reqDTO);

    /**
     * 用 POST 而非 GET：对象参数在 GET 下需要 Feign 专属的 {@code @SpringQueryMap}，
     * 服务端 Spring MVC 不识别该注解，两端绑定方式会不一致
     */
    @PostMapping(PREFIX + "/validate")
    @Operation(summary = "校验短信验证码，只校验不消费")
    CommonResult<Boolean> validateSmsCode(@Valid @RequestBody SmsCodeValidateReqDTO reqDTO);
}
