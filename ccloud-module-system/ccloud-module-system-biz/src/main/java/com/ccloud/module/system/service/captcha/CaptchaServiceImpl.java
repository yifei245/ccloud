package com.ccloud.module.system.service.captcha;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.module.system.controller.admin.captcha.vo.CaptchaRespVO;
import com.ccloud.module.system.dal.redis.auth.CaptchaRedisDAO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_CAPTCHA_CODE_ERROR;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final Duration TIMEOUT = Duration.ofMinutes(2);

    @Resource
    private CaptchaRedisDAO captchaRedisDAO;

    @Value("${ccloud.system.captcha.enable:true}")
    private boolean enabled;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public CaptchaRespVO createCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String uuid = IdUtil.fastSimpleUUID();
        captchaRedisDAO.set(uuid, captcha.getCode(), TIMEOUT);
        return new CaptchaRespVO(uuid, captcha.getImageBase64Data());
    }

    /**
     * 取出即删除：错误一次也要重新获取验证码，避免拿同一张图暴力尝试密码
     */
    @Override
    public void validate(String uuid, String code) {
        String expected = StrUtil.isNotEmpty(uuid) ? captchaRedisDAO.getAndDelete(uuid) : null;
        if (expected == null) {
            throw exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, "验证码已失效，请重新获取");
        }
        if (!expected.equalsIgnoreCase(code)) {
            throw exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, "验证码不正确");
        }
    }
}
