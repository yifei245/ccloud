package com.ccloud.module.system.framework.sms.core.client.impl;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.core.KeyValue;
import com.ccloud.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import com.ccloud.module.system.framework.sms.core.property.SmsChannelProperties;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;

import java.util.List;

/**
 * 腾讯云短信：apiKey 为「SecretId 空格 SdkAppId」（腾讯云发送需要应用编号，渠道表没有单独的列），apiSecret 为 SecretKey
 */
public class TencentSmsClient extends AbstractSmsClient {

    private static final String REGION = "ap-guangzhou";
    private static final String SUCCESS_CODE = "Ok";

    private volatile SmsClient client;
    private volatile String sdkAppId;

    public TencentSmsClient(SmsChannelProperties properties) {
        super(properties);
    }

    @Override
    protected void doInit() {
        List<String> parts = StrUtil.splitTrim(properties.getApiKey(), ' ');
        if (parts.size() != 2) {
            throw new IllegalArgumentException("腾讯云短信 apiKey 格式应为「SecretId SdkAppId」");
        }
        sdkAppId = parts.get(1);
        client = new SmsClient(new Credential(parts.get(0), properties.getApiSecret()), REGION);
    }

    @Override
    public SmsSendRespDTO sendSms(Long logId, String mobile, String apiTemplateId,
                                  List<KeyValue<String, Object>> templateParams) throws Exception {
        SendSmsResponse response = client.SendSms(buildRequest(logId, mobile, apiTemplateId, templateParams));
        SendStatus status = response.getSendStatusSet()[0];
        return new SmsSendRespDTO().setSuccess(SUCCESS_CODE.equals(status.getCode()))
                .setApiCode(status.getCode()).setApiMsg(status.getMessage())
                .setApiRequestId(response.getRequestId()).setSerialNo(status.getSerialNo());
    }

    /**
     * 腾讯云按位置传参，参数值须与模板中 {1}{2}... 的顺序一致
     */
    public SendSmsRequest buildRequest(Long logId, String mobile, String apiTemplateId,
                                       List<KeyValue<String, Object>> templateParams) {
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumberSet(new String[]{"+86" + mobile});
        request.setSmsSdkAppId(sdkAppId);
        request.setSignName(properties.getSignature());
        request.setTemplateId(apiTemplateId);
        request.setTemplateParamSet(templateParams.stream().map(param -> String.valueOf(param.getValue()))
                .toArray(String[]::new));
        request.setSessionContext(String.valueOf(logId));
        return request;
    }
}
