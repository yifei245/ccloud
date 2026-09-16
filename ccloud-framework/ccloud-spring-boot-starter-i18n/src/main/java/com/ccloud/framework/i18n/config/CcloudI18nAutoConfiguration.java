package com.ccloud.framework.i18n.config;

import com.ccloud.framework.i18n.core.DatabaseMessageSource;
import com.ccloud.framework.i18n.core.I18nUtils;
import com.ccloud.framework.i18n.core.resolver.HeaderLocaleResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;

/**
 * 国际化自动配置
 *
 * <p>两个 Bean 都必须用 Spring 约定的名字：ApplicationContext#getMessage 只委托名为
 * {@code messageSource} 的 Bean，DispatcherServlet 只认名为 {@code localeResolver} 的 Bean。
 * 同时要排在 Boot 自带配置之前，让它们的 @ConditionalOnMissingBean 生效而不是撞名启动失败。
 */
@AutoConfiguration(before = {WebMvcAutoConfiguration.class, MessageSourceAutoConfiguration.class})
@EnableConfigurationProperties(I18nProperties.class)
@ConditionalOnProperty(prefix = "ccloud.i18n", value = "enable", matchIfMissing = true)
public class CcloudI18nAutoConfiguration {

    @Bean(AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)
    @Primary
    public DatabaseMessageSource databaseMessageSource(I18nProperties properties) {
        ResourceBundleMessageSource baseline = new ResourceBundleMessageSource();
        baseline.setBasename(properties.getBasename());
        baseline.setDefaultEncoding("UTF-8");
        baseline.setUseCodeAsDefaultMessage(true);

        DatabaseMessageSource messageSource = new DatabaseMessageSource(baseline);
        I18nUtils.init(messageSource);
        return messageSource;
    }

    @Bean(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    public LocaleResolver localeResolver(I18nProperties properties) {
        return new HeaderLocaleResolver(properties);
    }
}
