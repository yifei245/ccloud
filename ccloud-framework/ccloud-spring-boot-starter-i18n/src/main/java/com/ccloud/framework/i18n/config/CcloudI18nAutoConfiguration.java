package com.ccloud.framework.i18n.config;

import com.ccloud.framework.i18n.core.DatabaseMessageSource;
import com.ccloud.framework.i18n.core.I18nUtils;
import com.ccloud.framework.i18n.core.resolver.HeaderLocaleResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;

/**
 * 国际化自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(I18nProperties.class)
@ConditionalOnProperty(prefix = "ccloud.i18n", value = "enable", matchIfMissing = true)
public class CcloudI18nAutoConfiguration {

    @Bean
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

    @Bean
    public LocaleResolver localeResolver(I18nProperties properties) {
        return new HeaderLocaleResolver(properties);
    }
}
