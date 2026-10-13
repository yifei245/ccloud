package com.ccloud.module.pay.framework.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PayPropertiesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PayProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void 默认关闭模拟支付_充值应用标识为wallet() {
        PayProperties properties = new PayProperties();
        assertThat(properties.isMockEnabled()).isFalse();
        assertThat(properties.getWallet().getRechargeAppKey()).isEqualTo("wallet");
        assertThat(properties.getJob().getBatchSize()).isEqualTo(100);
    }

    @Test
    void 合法配置可以绑定() {
        runner.withPropertyValues("ccloud.pay.job.batch-size=50", "ccloud.pay.wallet.recharge-app-key=w1")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(PayProperties.class).getJob().getBatchSize()).isEqualTo(50);
                });
    }

    @Test
    void 嵌套的任务批量为0时绑定失败() {
        runner.withPropertyValues("ccloud.pay.job.batch-size=0")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void 嵌套的充值应用标识为空时绑定失败() {
        runner.withPropertyValues("ccloud.pay.wallet.recharge-app-key=")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
