package com.ccloud.module.system.framework.sms;

import com.ccloud.framework.common.core.KeyValue;
import com.ccloud.module.system.framework.sms.core.client.SmsClient;
import com.ccloud.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import com.ccloud.module.system.framework.sms.core.client.impl.AliyunSmsClient;
import com.ccloud.module.system.framework.sms.core.client.impl.MockSmsClient;
import com.ccloud.module.system.framework.sms.core.client.impl.SmsClientFactoryImpl;
import com.ccloud.module.system.framework.sms.core.client.impl.TencentSmsClient;
import com.ccloud.module.system.framework.sms.core.property.SmsChannelProperties;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SmsClientTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final SmsClientFactoryImpl factory = new SmsClientFactoryImpl(redisTemplate);

    @Test
    void createOrUpdateSmsClient_配置不变时复用实例_变更后使用新配置() {
        SmsClient first = factory.createOrUpdateSmsClient(properties(1L, "MOCK", "签名A", "key"));
        SmsClient same = factory.createOrUpdateSmsClient(properties(1L, "MOCK", "签名A", "key"));
        assertThat(same).isSameAs(first);
        assertThat(factory.getSmsClient(1L)).isSameAs(first);

        MockSmsClient changed = (MockSmsClient) factory.createOrUpdateSmsClient(properties(1L, "MOCK", "签名B", "key"));
        assertThat(changed.getProperties().getSignature()).isEqualTo("签名B");
    }

    @Test
    void createOrUpdateSmsClient_渠道编码变更时换成对应实现() {
        factory.createOrUpdateSmsClient(properties(2L, "MOCK", "签名", "key"));
        SmsClient aliyun = factory.createOrUpdateSmsClient(properties(2L, "ALIYUN", "签名", "key"));
        assertThat(aliyun).isInstanceOf(AliyunSmsClient.class);
    }

    @Test
    void createOrUpdateSmsClient_未知渠道编码应拒绝() {
        assertThatThrownBy(() -> factory.createOrUpdateSmsClient(properties(3L, "UNKNOWN", "签名", "key")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mockSmsClient_不真正发送_参数写入Redis供联调读取() throws Throwable {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        SmsClient client = factory.createOrUpdateSmsClient(properties(4L, "MOCK", "签名", "key"));

        SmsSendRespDTO resp = client.sendSms(10L, "13800000000", "tpl", List.of(new KeyValue<>("code", "1234")));

        assertThat(resp.getSuccess()).isTrue();
        assertThat(resp.getSerialNo()).isEqualTo("10");
        verify(ops).set(eq("ccloud:sms_mock:13800000000"), eq("{\"code\":\"1234\"}"), eq(Duration.ofMinutes(30)));
    }

    @Test
    void aliyun_请求携带签名模板与JSON参数_outId为日志编号() {
        AliyunSmsClient client = new AliyunSmsClient(properties(5L, "ALIYUN", "阿里签名", "ak"));
        var request = client.buildRequest(11L, "13800000000", "SMS_1",
                List.of(new KeyValue<>("code", "1234"), new KeyValue<>("name", "张三")));

        assertThat(request.getPhoneNumbers()).isEqualTo("13800000000");
        assertThat(request.getSignName()).isEqualTo("阿里签名");
        assertThat(request.getTemplateCode()).isEqualTo("SMS_1");
        assertThat(request.getTemplateParam()).isEqualTo("{\"code\":\"1234\",\"name\":\"张三\"}");
        assertThat(request.getOutId()).isEqualTo("11");
    }

    @Test
    void tencent_apiKey为secretId与sdkAppId_参数按模板顺序传值() {
        TencentSmsClient client = new TencentSmsClient(properties(6L, "TENCENT", "腾讯签名", "sid 1400000000"));
        SendSmsRequest request = client.buildRequest(12L, "13800000000", "100",
                List.of(new KeyValue<>("code", "1234"), new KeyValue<>("minute", 5)));

        assertThat(request.getPhoneNumberSet()).containsExactly("+8613800000000");
        assertThat(request.getSmsSdkAppId()).isEqualTo("1400000000");
        assertThat(request.getSignName()).isEqualTo("腾讯签名");
        assertThat(request.getTemplateId()).isEqualTo("100");
        assertThat(request.getTemplateParamSet()).containsExactly("1234", "5");
        assertThat(request.getSessionContext()).isEqualTo("12");
    }

    @Test
    void tencent_apiKey格式不正确应拒绝() {
        assertThatThrownBy(() -> new TencentSmsClient(properties(7L, "TENCENT", "签名", "only-secret-id")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SmsChannelProperties properties(Long id, String code, String signature, String apiKey) {
        return new SmsChannelProperties().setId(id).setCode(code).setSignature(signature)
                .setApiKey(apiKey).setApiSecret("secret");
    }
}
