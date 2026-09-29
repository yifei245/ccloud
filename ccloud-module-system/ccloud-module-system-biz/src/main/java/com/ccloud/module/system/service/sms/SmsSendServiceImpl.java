package com.ccloud.module.system.service.sms;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.core.KeyValue;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.module.system.dal.dataobject.sms.SmsChannelDO;
import com.ccloud.module.system.dal.dataobject.sms.SmsLogDO;
import com.ccloud.module.system.dal.dataobject.sms.SmsTemplateDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.enums.sms.SmsSendStatusEnum;
import com.ccloud.module.system.framework.sms.core.client.SmsClient;
import com.ccloud.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import com.ccloud.module.system.mq.message.sms.SmsSendMessage;
import com.ccloud.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Slf4j
@Service
public class SmsSendServiceImpl implements SmsSendService {

    @Resource
    private SmsTemplateService smsTemplateService;
    @Resource
    private SmsChannelService smsChannelService;
    @Resource
    private SmsLogService smsLogService;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private OutboxMessageService outboxMessageService;

    @Override
    public Long sendSingleSmsToAdmin(String mobile, Long userId, String templateCode, Map<String, Object> templateParams) {
        if (StrUtil.isEmpty(mobile) && userId != null) {
            AdminUserDO user = adminUserService.getUser(userId);
            if (user != null) {
                mobile = user.getMobile();
            }
        }
        return sendSingleSms(mobile, userId, UserTypeEnum.ADMIN.getValue(), templateCode, templateParams);
    }

    @Override
    public Long sendSingleSmsToMember(String mobile, Long userId, String templateCode, Map<String, Object> templateParams) {
        return sendSingleSms(mobile, userId, UserTypeEnum.MEMBER.getValue(), templateCode, templateParams);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendSingleSms(String mobile, Long userId, Integer userType, String templateCode,
                              Map<String, Object> templateParams) {
        SmsTemplateDO template = smsTemplateService.getSmsTemplateByCode(templateCode);
        if (template == null) {
            throw exception(SMS_SEND_TEMPLATE_NOT_EXISTS);
        }
        if (StrUtil.isEmpty(mobile)) {
            throw exception(SMS_SEND_MOBILE_NOT_EXISTS);
        }
        List<KeyValue<String, Object>> orderedParams = buildTemplateParams(template, templateParams);

        SmsChannelDO channel = smsChannelService.getSmsChannel(template.getChannelId());
        boolean isSend = CommonStatusEnum.isEnable(template.getStatus())
                && channel != null && CommonStatusEnum.isEnable(channel.getStatus());
        String content = smsTemplateService.formatSmsTemplateContent(template.getContent(), templateParams);
        Long logId = smsLogService.createSmsLog(mobile, userId, userType, isSend, template, content, templateParams);
        if (isSend) {
            outboxMessageService.saveAndPublish(SmsSendMessage.TOPIC, new SmsSendMessage().setLogId(logId)
                    .setTenantId(TenantContextHolder.getTenantId()).setMobile(mobile)
                    .setChannelId(template.getChannelId()).setApiTemplateId(template.getApiTemplateId())
                    .setTemplateParams(orderedParams));
        }
        return logId;
    }

    /**
     * 按模板声明的参数顺序取值：部分渠道（腾讯云）按位置传参，顺序错了内容就错了
     */
    private static List<KeyValue<String, Object>> buildTemplateParams(SmsTemplateDO template, Map<String, Object> params) {
        List<KeyValue<String, Object>> result = new ArrayList<>();
        for (String key : template.getParams()) {
            Object value = params == null ? null : params.get(key);
            if (value == null) {
                throw exception(SMS_SEND_MOBILE_TEMPLATE_PARAM_MISS, key);
            }
            result.add(new KeyValue<>(key, value));
        }
        return result;
    }

    @Override
    public void doSendSms(SmsSendMessage message) {
        SmsLogDO smsLog = smsLogService.getSmsLog(message.getLogId());
        if (smsLog == null || !Objects.equals(smsLog.getSendStatus(), SmsSendStatusEnum.INIT.getStatus())) {
            log.info("[doSendSms] 短信日志 {} 不存在或已处理，跳过", message.getLogId());
            return;
        }
        SmsClient client = smsChannelService.getSmsClient(message.getChannelId());
        if (client == null) {
            smsLogService.updateSmsSendResult(message.getLogId(),
                    new SmsSendRespDTO().setSuccess(false).setApiMsg("短信渠道不存在"));
            return;
        }
        SmsSendRespDTO result;
        try {
            result = client.sendSms(message.getLogId(), message.getMobile(), message.getApiTemplateId(),
                    message.getTemplateParams());
        } catch (Throwable e) {
            // 记为失败而不抛出重投：验证码类短信有时效，延迟重发意义不大，还可能在用户已重新获取后送达旧码
            log.error("[doSendSms] 短信日志 {} 调用渠道异常", message.getLogId(), e);
            result = new SmsSendRespDTO().setSuccess(false).setApiMsg(StrUtil.blankToDefault(e.getMessage(), e.getClass().getName()));
        }
        smsLogService.updateSmsSendResult(message.getLogId(), result);
    }
}
