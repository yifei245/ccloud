package com.ccloud.module.system.controller.admin.captcha;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.controller.admin.captcha.vo.CaptchaRespVO;
import com.ccloud.module.system.service.captcha.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 登录页使用，免登录（见 SecurityConfiguration）
 */
@Tag(name = "管理后台 - 验证码")
@RestController
@RequestMapping("/admin-api/system/captcha")
@Validated
public class CaptchaController {

    @Resource
    private CaptchaService captchaService;

    @GetMapping("/enabled")
    @Operation(summary = "是否开启验证码", description = "登录页据此决定是否展示验证码")
    public CommonResult<Boolean> isEnabled() {
        return success(captchaService.isEnabled());
    }

    @GetMapping("/get")
    @Operation(summary = "获得图形验证码")
    public CommonResult<CaptchaRespVO> getCaptcha() {
        return success(captchaService.createCaptcha());
    }
}
