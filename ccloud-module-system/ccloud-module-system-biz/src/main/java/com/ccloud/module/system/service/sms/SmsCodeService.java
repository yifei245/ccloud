package com.ccloud.module.system.service.sms;

import com.ccloud.module.system.api.sms.dto.SmsCodeSendReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeUseReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeValidateReqDTO;
import jakarta.validation.Valid;

/**
 * 短信验证码 Service。同一手机号同一场景只认最近一条验证码
 */
public interface SmsCodeService {

    /**
     * 生成并发送验证码，受发送间隔与每日上限约束
     */
    void sendSmsCode(@Valid SmsCodeSendReqDTO reqDTO);

    /**
     * 校验通过后立即标记已使用，并发使用同一验证码只有一个成功
     */
    void useSmsCode(@Valid SmsCodeUseReqDTO reqDTO);

    /**
     * 只校验不消费，用于多步流程的前置校验
     */
    void validateSmsCode(@Valid SmsCodeValidateReqDTO reqDTO);
}
