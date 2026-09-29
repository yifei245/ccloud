package com.ccloud.module.system.service.captcha;

import com.ccloud.module.system.controller.admin.captcha.vo.CaptchaRespVO;
import com.ccloud.module.system.dal.redis.auth.CaptchaRedisDAO;
import com.ccloud.module.system.test.BaseSystemDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static com.ccloud.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_CAPTCHA_CODE_ERROR;
import static org.assertj.core.api.Assertions.assertThat;

@Import({CaptchaServiceImpl.class, CaptchaRedisDAO.class})
@TestPropertySource(properties = "ccloud.system.captcha.enable=true")
class CaptchaServiceImplTest extends BaseSystemDbAndRedisUnitTest {

    @Resource
    private CaptchaService captchaService;
    @Resource
    private CaptchaRedisDAO captchaRedisDAO;

    @Test
    void 生成验证码_返回图片与编号() {
        CaptchaRespVO respVO = captchaService.createCaptcha();

        assertThat(respVO.getUuid()).isNotBlank();
        assertThat(respVO.getImg()).startsWith("data:image/png;base64,");
        assertThat(captchaRedisDAO.get(respVO.getUuid())).isNotBlank();
    }

    @Test
    void 校验通过后验证码立即失效_不能重复使用() {
        CaptchaRespVO respVO = captchaService.createCaptcha();
        String code = captchaRedisDAO.get(respVO.getUuid());

        captchaService.validate(respVO.getUuid(), code);

        assertThat(captchaRedisDAO.get(respVO.getUuid())).isNull();
        assertServiceException(() -> captchaService.validate(respVO.getUuid(), code), AUTH_LOGIN_CAPTCHA_CODE_ERROR);
    }

    @Test
    void 校验不区分大小写_但错误的验证码不通过() {
        CaptchaRespVO respVO = captchaService.createCaptcha();
        String code = captchaRedisDAO.get(respVO.getUuid());

        assertServiceException(() -> captchaService.validate(respVO.getUuid(), code + "x"),
                AUTH_LOGIN_CAPTCHA_CODE_ERROR);
        // 错误一次后验证码同样失效，避免被暴力尝试
        assertServiceException(() -> captchaService.validate(respVO.getUuid(), code), AUTH_LOGIN_CAPTCHA_CODE_ERROR);

        CaptchaRespVO other = captchaService.createCaptcha();
        captchaService.validate(other.getUuid(), captchaRedisDAO.get(other.getUuid()).toUpperCase());
    }

    @Test
    void 验证码编号不存在() {
        assertServiceException(() -> captchaService.validate("none", "1234"), AUTH_LOGIN_CAPTCHA_CODE_ERROR);
    }

    @Test
    void 按配置开启验证码() {
        assertThat(captchaService.isEnabled()).isTrue();
    }
}
