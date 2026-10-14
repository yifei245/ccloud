package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MockPayClientProviderConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MockPayClientProvider.class, MockPayNotifySender.class)
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    @Test
    void 未开启模拟支付时不注册Mock渠道() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(MockPayClientProvider.class));
        runner.withPropertyValues("ccloud.pay.mock-enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(MockPayClientProvider.class));
    }

    @Test
    void 开启后注册() {
        runner.withPropertyValues("ccloud.pay.mock-enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(MockPayClientProvider.class));
    }
}
