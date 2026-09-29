package com.ccloud.framework.i18n.config;

import com.ccloud.framework.i18n.core.DatabaseMessageSource;
import com.ccloud.framework.i18n.core.resolver.HeaderLocaleResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 与 Spring MVC、Boot 自带 MessageSource 自动配置一起装配。
 *
 * <p>两处都靠 Bean 名字生效：DispatcherServlet 只认名为 {@code localeResolver} 的 Bean，
 * ApplicationContext#getMessage 只委托给名为 {@code messageSource} 的 Bean。
 */
class CcloudI18nAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    WebMvcAutoConfiguration.class,
                    MessageSourceAutoConfiguration.class,
                    CcloudI18nAutoConfiguration.class));

    @Test
    void 与WebMvc一起装配时应使用HeaderLocaleResolver() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class))
                    .isInstanceOf(HeaderLocaleResolver.class);
            assertThat(ctx).getBeans(LocaleResolver.class).hasSize(1);
        });
    }

    @Test
    void 应用上下文的getMessage应走数据库文案() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME))
                    .isInstanceOf(DatabaseMessageSource.class);
            ctx.getBean(DatabaseMessageSource.class).refresh("zh_CN", Map.of("hello", "你好"));
            assertThat(ctx.getMessage("hello", null, Locale.SIMPLIFIED_CHINESE)).isEqualTo("你好");
        });
    }
}
