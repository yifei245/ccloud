package com.ccloud.framework.i18n.core;

import com.ccloud.framework.i18n.config.I18nProperties;
import com.ccloud.framework.i18n.core.resolver.HeaderLocaleResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderLocaleResolverTest {

    private HeaderLocaleResolver resolver;

    @BeforeEach
    void setUp() {
        I18nProperties properties = new I18nProperties();
        properties.setDefaultLocale("zh_CN");
        properties.setSupportedLocales(List.of("zh_CN", "en_US"));
        resolver = new HeaderLocaleResolver(properties);
    }

    private Locale resolve(String acceptLanguage) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (acceptLanguage != null) {
            request.addHeader("Accept-Language", acceptLanguage);
        }
        return resolver.resolveLocale(request);
    }

    @Test
    @DisplayName("Accept-Language 为 en-US 时解析为英文")
    void shouldResolveEnglish() {
        assertThat(resolve("en-US")).isEqualTo(Locale.US);
    }

    @Test
    @DisplayName("Accept-Language 缺失时回退到默认语言")
    void shouldFallbackWhenHeaderMissing() {
        assertThat(resolve(null)).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }

    @Test
    @DisplayName("不支持的语言应回退到默认语言，而不是返回一个没有文案的 Locale")
    void shouldFallbackForUnsupportedLocale() {
        assertThat(resolve("ja-JP")).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }

    @Test
    @DisplayName("带权重的 Accept-Language 应挑出受支持的最高优先项")
    void shouldPickHighestSupportedFromWeightedHeader() {
        assertThat(resolve("ja-JP;q=0.9,en-US;q=0.8")).isEqualTo(Locale.US);
    }

    @Test
    @DisplayName("格式非法的头不应抛异常，回退到默认语言")
    void shouldTolerateMalformedHeader() {
        assertThat(resolve("!!!garbage!!!")).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }
}
