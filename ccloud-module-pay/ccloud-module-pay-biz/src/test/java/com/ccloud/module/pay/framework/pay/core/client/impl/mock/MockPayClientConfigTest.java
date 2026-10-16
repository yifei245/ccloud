package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockPayClientConfigTest {

    private static MockPayClientConfig valid() {
        return new MockPayClientConfig().setNotifySecret("0123456789abcdef").setBehavior(MockPayBehavior.SUCCESS).setDelaySeconds(3);
    }

    @Test
    void 合法配置通过() {
        assertThatCode(() -> valid().validate()).doesNotThrowAnyException();
        assertThatCode(() -> valid().setDelaySeconds(1).validate()).doesNotThrowAnyException();
        assertThatCode(() -> valid().setDelaySeconds(60).validate()).doesNotThrowAnyException();
    }

    @Test
    void 密钥为空或过短被拒绝() {
        assertThatThrownBy(() -> valid().setNotifySecret(null).validate()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> valid().setNotifySecret("").validate()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> valid().setNotifySecret("0123456789abcde").validate()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 延迟秒数越界或行为为空被拒绝() {
        assertThatThrownBy(() -> valid().setDelaySeconds(0).validate()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> valid().setDelaySeconds(61).validate()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> valid().setDelaySeconds(null).validate()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> valid().setBehavior(null).validate()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 敏感字段为回调密钥() {
        assertThat(valid().sensitiveFields()).isEqualTo(Set.of("notifySecret"));
    }
}
