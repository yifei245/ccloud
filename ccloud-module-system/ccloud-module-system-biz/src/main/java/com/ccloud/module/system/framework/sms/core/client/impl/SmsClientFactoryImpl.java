package com.ccloud.module.system.framework.sms.core.client.impl;

import com.ccloud.module.system.enums.sms.SmsChannelEnum;
import com.ccloud.module.system.framework.sms.core.client.SmsClient;
import com.ccloud.module.system.framework.sms.core.client.SmsClientFactory;
import com.ccloud.module.system.framework.sms.core.property.SmsChannelProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SmsClientFactoryImpl implements SmsClientFactory {

    private final Map<Long, AbstractSmsClient> clients = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;

    public SmsClientFactoryImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public SmsClient getSmsClient(Long channelId) {
        return clients.get(channelId);
    }

    @Override
    public SmsClient createOrUpdateSmsClient(SmsChannelProperties properties) {
        return clients.compute(properties.getId(), (id, client) -> {
            // 渠道编码变了，实现类也跟着变，只能重建
            if (client == null || !Objects.equals(client.getProperties().getCode(), properties.getCode())) {
                return createSmsClient(properties);
            }
            client.refresh(properties);
            return client;
        });
    }

    private AbstractSmsClient createSmsClient(SmsChannelProperties properties) {
        SmsChannelEnum channel = SmsChannelEnum.getByCode(properties.getCode());
        if (channel == null) {
            throw new IllegalArgumentException("未知的短信渠道编码：" + properties.getCode());
        }
        return switch (channel) {
            case MOCK -> new MockSmsClient(properties, redisTemplate);
            case ALIYUN -> new AliyunSmsClient(properties);
            case TENCENT -> new TencentSmsClient(properties);
        };
    }
}
