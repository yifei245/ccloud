package com.ccloud.module.system.framework.sms.core.client.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.ccloud.framework.common.core.KeyValue;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import com.ccloud.module.system.framework.sms.core.property.SmsChannelProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云短信：apiKey 为 AccessKey ID，apiSecret 为 AccessKey Secret
 */
public class AliyunSmsClient extends AbstractSmsClient {

    private static final String ENDPOINT = "dysmsapi.aliyuncs.com";
    private static final String SUCCESS_CODE = "OK";

    private volatile Client client;

    public AliyunSmsClient(SmsChannelProperties properties) {
        super(properties);
    }

    @Override
    protected void doInit() {
        try {
            client = new Client(new Config().setAccessKeyId(properties.getApiKey())
                    .setAccessKeySecret(properties.getApiSecret()).setEndpoint(ENDPOINT));
        } catch (Exception e) {
            throw new IllegalStateException("阿里云短信客户端初始化失败", e);
        }
    }

    @Override
    public SmsSendRespDTO sendSms(Long logId, String mobile, String apiTemplateId,
                                  List<KeyValue<String, Object>> templateParams) throws Exception {
        SendSmsResponseBody body = client.sendSms(buildRequest(logId, mobile, apiTemplateId, templateParams)).getBody();
        return new SmsSendRespDTO().setSuccess(SUCCESS_CODE.equals(body.getCode()))
                .setApiCode(body.getCode()).setApiMsg(body.getMessage())
                .setApiRequestId(body.getRequestId()).setSerialNo(body.getBizId());
    }

    /**
     * 阿里云按参数名传参，模板参数为 JSON 对象
     */
    public SendSmsRequest buildRequest(Long logId, String mobile, String apiTemplateId,
                                       List<KeyValue<String, Object>> templateParams) {
        Map<String, Object> params = new LinkedHashMap<>();
        templateParams.forEach(param -> params.put(param.getKey(), param.getValue()));
        return new SendSmsRequest().setPhoneNumbers(mobile).setSignName(properties.getSignature())
                .setTemplateCode(apiTemplateId).setTemplateParam(JsonUtils.toJsonString(params))
                .setOutId(String.valueOf(logId));
    }
}
