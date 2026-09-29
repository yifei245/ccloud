package com.ccloud.module.system.service.captcha;

import com.ccloud.module.system.controller.admin.captcha.vo.CaptchaRespVO;

/**
 * 图形验证码 Service
 */
public interface CaptchaService {

    /**
     * 是否开启验证码。关闭时登录不校验验证码
     */
    boolean isEnabled();

    CaptchaRespVO createCaptcha();

    /**
     * 校验验证码；无论成功失败都使其立即失效
     */
    void validate(String uuid, String code);
}
