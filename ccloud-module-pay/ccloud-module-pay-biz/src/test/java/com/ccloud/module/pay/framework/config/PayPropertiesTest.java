package com.ccloud.module.pay.framework.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayPropertiesTest {

    @Test
    void 默认关闭模拟支付_充值应用标识为wallet() {
        PayProperties properties = new PayProperties();
        assertThat(properties.isMockEnabled()).isFalse();
        assertThat(properties.getWallet().getRechargeAppKey()).isEqualTo("wallet");
        assertThat(properties.getJob().getBatchSize()).isEqualTo(100);
    }
}
