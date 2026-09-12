package com.ccloud.framework.i18n.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 国际化配置
 */
@ConfigurationProperties(prefix = "ccloud.i18n")
@Data
public class I18nProperties {

    private boolean enable = true;

    private String defaultLocale = "zh_CN";

    /**
     * 支持的语言。不在列表内的一律回退到默认语言——
     * 返回一个没有任何文案的 Locale 只会让界面全是 code
     */
    private List<String> supportedLocales = List.of("zh_CN", "en_US");

    private String basename = "i18n/messages";
}
