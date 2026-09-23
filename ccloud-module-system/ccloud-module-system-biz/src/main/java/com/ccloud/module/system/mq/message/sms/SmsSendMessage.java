package com.ccloud.module.system.mq.message.sms;

import com.ccloud.framework.common.core.KeyValue;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 短信发送消息：日志已落库，消费方调用渠道发送并回写结果
 */
@Data
@Accessors(chain = true)
public class SmsSendMessage {

    public static final String TOPIC = "system.sms.send";

    private Long logId;
    /**
     * 短信日志所属租户，消费线程据此切换租户上下文
     */
    private Long tenantId;
    private String mobile;
    private Long channelId;
    private String apiTemplateId;
    /**
     * 按模板参数声明顺序排列
     */
    private List<KeyValue<String, Object>> templateParams;
}
