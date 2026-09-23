package com.ccloud.module.system.service.sms;

import com.ccloud.module.system.api.sms.dto.SmsCodeSendReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeUseReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeValidateReqDTO;
import com.ccloud.module.system.dal.dataobject.sms.SmsCodeDO;
import com.ccloud.module.system.dal.mysql.sms.SmsCodeMapper;
import com.ccloud.module.system.dal.redis.sms.SmsCodeFailCountRedisDAO;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import com.ccloud.module.system.framework.sms.config.SmsCodeProperties;
import com.ccloud.module.system.test.BaseSystemDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Map;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@Import({SmsCodeServiceImpl.class, SmsCodeFailCountRedisDAO.class})
@EnableConfigurationProperties(SmsCodeProperties.class)
@TestPropertySource(properties = {
        "ccloud.sms-code.expire-times=10m",
        "ccloud.sms-code.send-frequency=1m",
        "ccloud.sms-code.send-maximum-quantity-per-day=3",
        "ccloud.sms-code.begin-code=123456",
        "ccloud.sms-code.end-code=123456",
        "ccloud.sms-code.max-validate-fail-times=3"
})
class SmsCodeServiceImplTest extends BaseSystemDbAndRedisUnitTest {

    private static final String MOBILE = "13800000000";
    private static final Integer SCENE = SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene();

    @Resource
    private SmsCodeService smsCodeService;
    @Resource
    private SmsCodeMapper smsCodeMapper;

    @MockitoBean
    private SmsSendService smsSendService;

    // ========== 发送 ==========

    @Test
    void sendSmsCode_成功时落库并按场景模板发送() {
        smsCodeService.sendSmsCode(sendReqDTO());

        SmsCodeDO code = smsCodeMapper.selectLastByMobile(MOBILE, SCENE);
        assertThat(code.getCode()).isEqualTo("123456");
        assertThat(code.getTodayIndex()).isEqualTo(1);
        assertThat(code.getUsed()).isFalse();
        assertThat(code.getCreateIp()).isEqualTo("10.0.0.1");
        verify(smsSendService).sendSingleSms(eq(MOBILE), isNull(), isNull(), eq("admin-sms-login"),
                eq(Map.of("code", "123456")));
    }

    @Test
    void sendSmsCode_发送间隔内重复发送应失败() {
        smsCodeService.sendSmsCode(sendReqDTO());
        assertServiceException(() -> smsCodeService.sendSmsCode(sendReqDTO()), SMS_CODE_SEND_TOO_FAST);
    }

    @Test
    void sendSmsCode_不同场景各自计算间隔() {
        smsCodeService.sendSmsCode(sendReqDTO());
        SmsCodeSendReqDTO other = sendReqDTO();
        other.setScene(SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene());
        smsCodeService.sendSmsCode(other);
    }

    @Test
    void sendSmsCode_当日超上限应失败() {
        insertCode("111111", LocalDateTime.now().minusMinutes(5), 3, false);
        assertServiceException(() -> smsCodeService.sendSmsCode(sendReqDTO()), SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY);
    }

    @Test
    void sendSmsCode_跨天后当日序号重新计数() {
        insertCode("111111", LocalDateTime.now().minusDays(1), 3, false);
        smsCodeService.sendSmsCode(sendReqDTO());
        assertThat(smsCodeMapper.selectLastByMobile(MOBILE, SCENE).getTodayIndex()).isEqualTo(1);
    }

    // ========== 校验 ==========

    @Test
    void validateSmsCode_正确时通过且不消费() {
        insertCode("654321", LocalDateTime.now(), 1, false);
        smsCodeService.validateSmsCode(validateReqDTO("654321"));
        smsCodeService.validateSmsCode(validateReqDTO("654321"));
    }

    @Test
    void validateSmsCode_未发送过() {
        assertServiceException(() -> smsCodeService.validateSmsCode(validateReqDTO("654321")), SMS_CODE_NOT_FOUND);
    }

    @Test
    void validateSmsCode_验证码错误() {
        insertCode("654321", LocalDateTime.now(), 1, false);
        assertServiceException(() -> smsCodeService.validateSmsCode(validateReqDTO("000000")), SMS_CODE_NOT_FOUND);
    }

    @Test
    void validateSmsCode_已过期() {
        insertCode("654321", LocalDateTime.now().minusMinutes(11), 1, false);
        assertServiceException(() -> smsCodeService.validateSmsCode(validateReqDTO("654321")), SMS_CODE_EXPIRED);
    }

    @Test
    void validateSmsCode_已使用() {
        insertCode("654321", LocalDateTime.now(), 1, true);
        assertServiceException(() -> smsCodeService.validateSmsCode(validateReqDTO("654321")), SMS_CODE_USED);
    }

    @Test
    void validateSmsCode_只认最近一条_旧验证码失效() {
        insertCode("111111", LocalDateTime.now().minusMinutes(3), 1, false);
        insertCode("222222", LocalDateTime.now(), 2, false);
        assertServiceException(() -> smsCodeService.validateSmsCode(validateReqDTO("111111")), SMS_CODE_NOT_FOUND);
    }

    @Test
    void validateSmsCode_错误次数达到上限后正确验证码也不可用_重新获取后恢复() {
        insertCode("654321", LocalDateTime.now().minusMinutes(2), 1, false);
        for (int i = 0; i < 3; i++) {
            assertServiceException(() -> smsCodeService.validateSmsCode(validateReqDTO("000000")), SMS_CODE_NOT_FOUND);
        }
        assertServiceException(() -> smsCodeService.validateSmsCode(validateReqDTO("654321")),
                SMS_CODE_VALIDATE_FAIL_TOO_MANY);

        smsCodeService.sendSmsCode(sendReqDTO());
        smsCodeService.validateSmsCode(validateReqDTO("123456"));
    }

    // ========== 使用 ==========

    @Test
    void useSmsCode_成功后标记已使用且不可复用() {
        insertCode("654321", LocalDateTime.now(), 1, false);

        smsCodeService.useSmsCode(useReqDTO("654321"));

        SmsCodeDO code = smsCodeMapper.selectLastByMobile(MOBILE, SCENE);
        assertThat(code.getUsed()).isTrue();
        assertThat(code.getUsedIp()).isEqualTo("10.0.0.2");
        assertThat(code.getUsedTime()).isNotNull();
        assertServiceException(() -> smsCodeService.useSmsCode(useReqDTO("654321")), SMS_CODE_USED);
    }

    @Test
    void useSmsCode_验证码错误时不标记() {
        insertCode("654321", LocalDateTime.now(), 1, false);
        assertServiceException(() -> smsCodeService.useSmsCode(useReqDTO("000000")), SMS_CODE_NOT_FOUND);
        assertThat(smsCodeMapper.selectLastByMobile(MOBILE, SCENE).getUsed()).isFalse();
    }

    private void insertCode(String code, LocalDateTime createTime, int todayIndex, boolean used) {
        SmsCodeDO codeDO = new SmsCodeDO().setMobile(MOBILE).setCode(code).setScene(SCENE).setCreateIp("10.0.0.1")
                .setTodayIndex(todayIndex).setUsed(used);
        codeDO.setCreateTime(createTime);
        smsCodeMapper.insert(codeDO);
    }

    private static SmsCodeSendReqDTO sendReqDTO() {
        SmsCodeSendReqDTO reqDTO = new SmsCodeSendReqDTO();
        reqDTO.setMobile(MOBILE);
        reqDTO.setScene(SCENE);
        reqDTO.setCreateIp("10.0.0.1");
        return reqDTO;
    }

    private static SmsCodeValidateReqDTO validateReqDTO(String code) {
        SmsCodeValidateReqDTO reqDTO = new SmsCodeValidateReqDTO();
        reqDTO.setMobile(MOBILE);
        reqDTO.setScene(SCENE);
        reqDTO.setCode(code);
        return reqDTO;
    }

    private static SmsCodeUseReqDTO useReqDTO(String code) {
        SmsCodeUseReqDTO reqDTO = new SmsCodeUseReqDTO();
        reqDTO.setMobile(MOBILE);
        reqDTO.setScene(SCENE);
        reqDTO.setCode(code);
        reqDTO.setUsedIp("10.0.0.2");
        return reqDTO;
    }
}
