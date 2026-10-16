package com.ccloud.module.pay.framework.pay.core.client;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.module.pay.support.FakePayClient;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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

    @Test
    void 配置不是合法JSON时拒绝且配置内容不进日志() {
        // 末尾的 x 使解析在含密钥的位置之后失败，解析器的错误信息会带上原文片段
        String malformed = "{\"key\":\"a\",\"secret\":\"s3cr3t-value\"x}";
        List<ILoggingEvent> events = captureLogs(() -> {
            assertThatThrownBy(() -> factory.parseConfig("mock", malformed))
                    .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(CHANNEL_CONFIG_INVALID.getCode());
            assertThatThrownBy(() -> factory.maskConfig("mock", malformed))
                    .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(CHANNEL_CONFIG_INVALID.getCode());
        });
        assertThat(events).extracting(PayClientFactoryTest::render).noneMatch(text -> text.contains("s3cr3t-value"));
    }

    /** 收集执行期间所有日志事件（根 logger，含 JsonUtils 等 */
    private static List<ILoggingEvent> captureLogs(Runnable action) {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            action.run();
        } finally {
            root.detachAppender(appender);
        }
        return appender.list;
    }

    /** 日志消息与整条异常链的消息拼成一段文本，便于检查敏感内容 */
    private static String render(ILoggingEvent event) {
        StringBuilder text = new StringBuilder(event.getFormattedMessage());
        for (var t = event.getThrowableProxy(); t != null; t = t.getCause()) {
            text.append(' ').append(t.getMessage());
        }
        return text.toString();
    }
}
