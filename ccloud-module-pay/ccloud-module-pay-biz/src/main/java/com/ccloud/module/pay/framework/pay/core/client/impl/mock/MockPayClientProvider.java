package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

import com.ccloud.module.pay.enums.PayChannelEnum;
import com.ccloud.module.pay.framework.pay.core.client.PayClient;
import com.ccloud.module.pay.framework.pay.core.client.PayClientConfig;
import com.ccloud.module.pay.framework.pay.core.client.PayClientProvider;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 仅在 ccloud.pay.mock-enabled=true 时注册，生产环境不存在 Mock 渠道
 */
@Component
@ConditionalOnProperty(prefix = "ccloud.pay", name = "mock-enabled", havingValue = "true")
public class MockPayClientProvider implements PayClientProvider {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MockPayNotifySender sender;

    @Override
    public String getCode() {
        return PayChannelEnum.MOCK.getCode();
    }

    @Override
    public Class<? extends PayClientConfig> getConfigClass() {
        return MockPayClientConfig.class;
    }

    @Override
    public PayClient create(Long channelId, PayClientConfig config) {
        return new MockPayClient(channelId, (MockPayClientConfig) config, stringRedisTemplate, sender);
    }
}
