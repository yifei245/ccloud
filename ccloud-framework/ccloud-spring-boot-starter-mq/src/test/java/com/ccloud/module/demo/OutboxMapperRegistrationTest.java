package com.ccloud.module.demo;

import com.ccloud.framework.mq.outbox.core.ConsumeLogMapper;
import com.ccloud.framework.mq.outbox.core.IdempotentConsumer;
import com.ccloud.framework.mq.outbox.core.OutboxMessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模拟真实业务服务：启动类位于 com.ccloud.module.* 下，框架包不在自动配置包扫描范围内。
 * 框架自带的 Mapper 必须由 starter 自己注册，否则服务启动即失败。
 */
@SpringBootTest(classes = OutboxMapperRegistrationTest.DemoApp.class)
@ActiveProfiles("unit-test")
class OutboxMapperRegistrationTest {

    @Configuration
    @EnableAutoConfiguration
    static class DemoApp {
    }

    @Autowired
    private ApplicationContext ctx;

    @Test
    void 业务包下启动时框架Mapper应被注册() {
        assertThat(ctx.getBeansOfType(OutboxMessageMapper.class)).hasSize(1);
        assertThat(ctx.getBeansOfType(ConsumeLogMapper.class)).hasSize(1);
        assertThat(ctx.getBeansOfType(IdempotentConsumer.class)).hasSize(1);
    }
}
