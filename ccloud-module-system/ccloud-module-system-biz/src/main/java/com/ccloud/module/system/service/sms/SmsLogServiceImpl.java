package com.ccloud.module.system.service.sms;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.sms.vo.log.SmsLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.sms.SmsLogDO;
import com.ccloud.module.system.dal.dataobject.sms.SmsTemplateDO;
import com.ccloud.module.system.dal.mysql.sms.SmsLogMapper;
import com.ccloud.module.system.enums.sms.SmsSendStatusEnum;
import com.ccloud.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SmsLogServiceImpl implements SmsLogService {

    /**
     * 接收状态：初始化（渠道回执尚未到达）
     */
    private static final int RECEIVE_STATUS_INIT = 0;

    @Resource
    private SmsLogMapper smsLogMapper;

    @Override
    public Long createSmsLog(String mobile, Long userId, Integer userType, boolean isSend,
                             SmsTemplateDO template, String templateContent, Map<String, Object> templateParams) {
        SmsLogDO log = new SmsLogDO()
                .setChannelId(template.getChannelId()).setChannelCode(template.getChannelCode())
                .setTemplateId(template.getId()).setTemplateCode(template.getCode()).setTemplateType(template.getType())
                .setTemplateContent(templateContent).setTemplateParams(templateParams)
                .setApiTemplateId(template.getApiTemplateId())
                .setMobile(mobile).setUserId(userId).setUserType(userType)
                .setSendStatus(isSend ? SmsSendStatusEnum.INIT.getStatus() : SmsSendStatusEnum.IGNORE.getStatus())
                .setReceiveStatus(RECEIVE_STATUS_INIT);
        smsLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public void updateSmsSendResult(Long id, SmsSendRespDTO result) {
        smsLogMapper.updateById(new SmsLogDO().setId(id)
                .setSendStatus(Boolean.TRUE.equals(result.getSuccess())
                        ? SmsSendStatusEnum.SUCCESS.getStatus() : SmsSendStatusEnum.FAILURE.getStatus())
                .setSendTime(LocalDateTime.now())
                .setApiSendCode(result.getApiCode()).setApiSendMsg(StrUtil.maxLength(result.getApiMsg(), 250))
                .setApiRequestId(result.getApiRequestId()).setApiSerialNo(result.getSerialNo()));
    }

    @Override
    public SmsLogDO getSmsLog(Long id) {
        return smsLogMapper.selectById(id);
    }

    @Override
    public PageResult<SmsLogDO> getSmsLogPage(SmsLogPageReqVO pageReqVO) {
        return smsLogMapper.selectPage(pageReqVO);
    }
}
