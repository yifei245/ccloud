package com.ccloud.module.system.service.sms;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.sms.vo.log.SmsLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.sms.SmsLogDO;
import com.ccloud.module.system.dal.dataobject.sms.SmsTemplateDO;
import com.ccloud.module.system.framework.sms.core.client.dto.SmsSendRespDTO;

import java.util.Map;

/**
 * 短信日志 Service
 */
public interface SmsLogService {

    /**
     * 发送前落库
     *
     * @param isSend 是否发送；模板或渠道关闭时记为「不发送」，留下被拦截的记录
     * @return 日志编号
     */
    Long createSmsLog(String mobile, Long userId, Integer userType, boolean isSend,
                      SmsTemplateDO template, String templateContent, Map<String, Object> templateParams);

    /**
     * 回写渠道受理结果
     */
    void updateSmsSendResult(Long id, SmsSendRespDTO result);

    SmsLogDO getSmsLog(Long id);

    PageResult<SmsLogDO> getSmsLogPage(SmsLogPageReqVO pageReqVO);
}
