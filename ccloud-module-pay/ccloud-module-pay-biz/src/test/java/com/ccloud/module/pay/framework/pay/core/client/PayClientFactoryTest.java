package com.ccloud.module.pay.framework.pay.core.client;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.module.pay.support.FakePayClient;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.CHANNEL_CONFIG_INVALID;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.CHANNEL_NOT_SUPPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayClientFactoryTest {

    /** 测试用渠道：记录创建次数，配置 key 不能为空，secret 为敏感字段 */
    static class CountingProvider implements PayClientProvider {
        final AtomicInteger created = new AtomicInteger();
        final String code;
        CountingProvider(String code) { this.code = code; }
        @Override public String getCode() { return code; }
        @Override public Class<? extends PayClientConfig> getConfigClass() { return TestConfig.class; }
        @Override public PayClient create(Long channelId, PayClientConfig config) {
            created.incrementAndGet();
            FakePayClient client = new FakePayClient();
            client.channelId = channelId;
            return client;
        }
    }

    @Data
    public static class TestConfig implements PayClientConfig {
        private String key;
        private String secret;
        @Override public void validate() { if (StrUtil.isBlank(key)) throw new IllegalArgumentException("key 不能为空"); }
        @Override public Set<String> sensitiveFields() { return Set.of("secret"); }
    }

    private final CountingProvider provider = new CountingProvider("mock");
    private final PayClientFactory factory = new PayClientFactory(List.of(provider));

    @Test
    void 按编号缓存_配置不变时返回同一实例() {
        PayClient a = factory.getPayClient(1L, "mock", "{\"key\":\"a\"}");
        PayClient b = factory.getPayClient(1L, "mock", "{\"key\":\"a\"}");
        assertThat(b).isSameAs(a);
        assertThat(provider.created).hasValue(1);
    }

    @Test
    void 配置变化时重建() {
        PayClient a = factory.getPayClient(1L, "mock", "{\"key\":\"a\"}");
        PayClient b = factory.getPayClient(1L, "mock", "{\"key\":\"b\"}");
        assertThat(b).isNotSameAs(a);
        assertThat(provider.created).hasValue(2);
    }

    @Test
    void 未实现的编码抛渠道未实现() {
        assertThat(factory.isSupported("wx_lite")).isFalse();
        assertThatThrownBy(() -> factory.parseConfig("wx_lite", "{}"))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(CHANNEL_NOT_SUPPORTED.getCode());
    }

    @Test
    void 配置校验失败抛配置不正确且带原因() {
        assertThatThrownBy(() -> factory.parseConfig("mock", "{\"key\":\"\"}"))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> {
                    assertThat(((ServiceException) e).getCode()).isEqualTo(CHANNEL_CONFIG_INVALID.getCode());
                    assertThat(e.getMessage()).contains("key 不能为空");
                });
        assertThatThrownBy(() -> factory.parseConfig("mock", "not-json"))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(CHANNEL_CONFIG_INVALID.getCode());
    }

    @Test
    void 重复注册同一编码启动失败() {
        assertThatThrownBy(() -> new PayClientFactory(List.of(provider, new CountingProvider("mock"))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("mock");
    }

    @Test
    void 敏感字段回显为掩码_保存掩码时保留原值() {
        String masked = factory.maskConfig("mock", "{\"key\":\"a\",\"secret\":\"s3cr3t\"}");
        assertThat(masked).contains("\"secret\":\"***\"").contains("\"key\":\"a\"").doesNotContain("s3cr3t");

        String merged = factory.mergeMaskedConfig("mock", "{\"key\":\"b\",\"secret\":\"***\"}", "{\"key\":\"a\",\"secret\":\"s3cr3t\"}");
        assertThat(merged).contains("\"secret\":\"s3cr3t\"").contains("\"key\":\"b\"");

        String replaced = factory.mergeMaskedConfig("mock", "{\"key\":\"b\",\"secret\":\"new\"}", "{\"key\":\"a\",\"secret\":\"s3cr3t\"}");
        assertThat(replaced).contains("\"secret\":\"new\"");
    }
}
