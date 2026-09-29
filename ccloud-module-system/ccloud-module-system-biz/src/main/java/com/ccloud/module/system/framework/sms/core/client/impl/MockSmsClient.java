package com.ccloud.module.system.framework.sms.core.client.impl;

import com.ccloud.framework.common.core.KeyValue;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import com.ccloud.module.system.framework.sms.core.property.SmsChannelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟渠道：不真正发送。参数写日志并写入 Redis（{@code ccloud:sms_mock:{mobile}}），
 * 本地联调与端到端测试从 Redis 读取验证码
 */
@Slf4j
public class MockSmsClient extends AbstractSmsClient {

    static final String REDIS_KEY = "ccloud:sms_mock:%s";
    private static final Duration TIMEOUT = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public MockSmsClient(SmsChannelProperties properties, StringRedisTemplate redisTemplate) {
        super(properties);
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doInit() {
    }

    @Override
    public SmsSendRespDTO sendSms(Long logId, String mobile, String apiTemplateId,
                                  List<KeyValue<String, Object>> templateParams) {
        Map<String, Object> params = new LinkedHashMap<>();
        templateParams.forEach(param -> params.put(param.getKey(), param.getValue()));
        String json = JsonUtils.toJsonString(params);
        log.info("[sendSms][模拟发送] 日志 {} 手机号 {} 模板 {} 参数 {}", logId, mobile, apiTemplateId, json);
        redisTemplate.opsForValue().set(String.format(REDIS_KEY, mobile), json, TIMEOUT);
        return new SmsSendRespDTO().setSuccess(true).setApiCode("OK").setApiMsg("模拟发送")
                .setSerialNo(String.valueOf(logId));
    }
}
