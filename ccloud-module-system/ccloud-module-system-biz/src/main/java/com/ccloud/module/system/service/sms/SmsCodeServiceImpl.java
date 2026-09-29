package com.ccloud.module.system.service.sms;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.RandomUtil;
import com.ccloud.module.system.api.sms.dto.SmsCodeSendReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeUseReqDTO;
import com.ccloud.module.system.api.sms.dto.SmsCodeValidateReqDTO;
import com.ccloud.module.system.dal.dataobject.sms.SmsCodeDO;
import com.ccloud.module.system.dal.mysql.sms.SmsCodeMapper;
import com.ccloud.module.system.dal.redis.sms.SmsCodeFailCountRedisDAO;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import com.ccloud.module.system.framework.sms.config.SmsCodeProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SmsCodeServiceImpl implements SmsCodeService {

    @Resource
    private SmsCodeProperties smsCodeProperties;
    @Resource
    private SmsCodeMapper smsCodeMapper;
    @Resource
    private SmsCodeFailCountRedisDAO smsCodeFailCountRedisDAO;
    @Resource
    private SmsSendService smsSendService;

    @Override
    public void sendSmsCode(SmsCodeSendReqDTO reqDTO) {
        SmsSceneEnum scene = SmsSceneEnum.valueOf(reqDTO.getScene());
        String code = createSmsCode(reqDTO.getMobile(), reqDTO.getScene(), reqDTO.getCreateIp());
        smsSendService.sendSingleSms(reqDTO.getMobile(), null, null, scene.getTemplateCode(), Map.of("code", code));
    }

    private String createSmsCode(String mobile, Integer scene, String ip) {
        SmsCodeDO last = smsCodeMapper.selectLastByMobile(mobile, scene);
        boolean sentToday = last != null && LocalDateTimeUtil.isSameDay(last.getCreateTime(), LocalDateTime.now());
        if (last != null) {
            if (last.getCreateTime().plus(smsCodeProperties.getSendFrequency()).isAfter(LocalDateTime.now())) {
                throw exception(SMS_CODE_SEND_TOO_FAST);
            }
            if (sentToday && last.getTodayIndex() >= smsCodeProperties.getSendMaximumQuantityPerDay()) {
                throw exception(SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY);
            }
        }
        String code = String.valueOf(RandomUtil.randomInt(smsCodeProperties.getBeginCode(),
                smsCodeProperties.getEndCode() + 1));
        smsCodeMapper.insert(new SmsCodeDO().setMobile(mobile).setCode(code).setScene(scene).setCreateIp(ip)
                .setTodayIndex(sentToday ? last.getTodayIndex() + 1 : 1).setUsed(false));
        return code;
    }

    @Override
    public void useSmsCode(SmsCodeUseReqDTO reqDTO) {
        SmsCodeDO last = validateSmsCode0(reqDTO.getMobile(), reqDTO.getScene(), reqDTO.getCode());
        int updated = smsCodeMapper.updateToUsed(last.getId(), new SmsCodeDO().setUsed(true)
                .setUsedTime(LocalDateTime.now()).setUsedIp(reqDTO.getUsedIp()));
        if (updated == 0) {
            throw exception(SMS_CODE_USED);
        }
    }

    @Override
    public void validateSmsCode(SmsCodeValidateReqDTO reqDTO) {
        validateSmsCode0(reqDTO.getMobile(), reqDTO.getScene(), reqDTO.getCode());
    }

    private SmsCodeDO validateSmsCode0(String mobile, Integer scene, String code) {
        SmsCodeDO last = smsCodeMapper.selectLastByMobile(mobile, scene);
        if (last == null) {
            throw exception(SMS_CODE_NOT_FOUND);
        }
        // 先判断错误次数：达到上限后即使输对也拒绝，否则攻击者可以一直试到命中
        if (smsCodeFailCountRedisDAO.get(last.getId()) >= smsCodeProperties.getMaxValidateFailTimes()) {
            throw exception(SMS_CODE_VALIDATE_FAIL_TOO_MANY);
        }
        if (!last.getCode().equals(code)) {
            smsCodeFailCountRedisDAO.increment(last.getId(), smsCodeProperties.getExpireTimes());
            throw exception(SMS_CODE_NOT_FOUND);
        }
        if (last.getCreateTime().plus(smsCodeProperties.getExpireTimes()).isBefore(LocalDateTime.now())) {
            throw exception(SMS_CODE_EXPIRED);
        }
        if (Boolean.TRUE.equals(last.getUsed())) {
            throw exception(SMS_CODE_USED);
        }
        return last;
    }
}
