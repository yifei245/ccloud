package com.ccloud.module.system.framework.sms.core.client;

import com.ccloud.framework.common.core.KeyValue;
import com.ccloud.module.system.framework.sms.core.client.dto.SmsSendRespDTO;

import java.util.List;

/**
 * 短信客户端，每个短信渠道一个实例
 */
public interface SmsClient {

    /**
     * @return 渠道编号
     */
    Long getId();

    /**
     * 发送短信
     *
     * @param logId          短信日志编号，作为渠道侧的业务流水号，便于对账与回执关联
     * @param apiTemplateId  渠道侧的模板编号
     * @param templateParams 模板参数，按模板参数声明顺序排列（部分渠道按位置传参）
     * @return 渠道的受理结果；调用异常时直接抛出
     */
    SmsSendRespDTO sendSms(Long logId, String mobile, String apiTemplateId,
                           List<KeyValue<String, Object>> templateParams) throws Throwable;
}
