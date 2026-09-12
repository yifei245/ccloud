package com.ccloud.framework.i18n.core.resolver;

import com.ccloud.framework.i18n.config.I18nProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.LocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * 依据 {@code Accept-Language} 头解析语言
 */
@Slf4j
public class HeaderLocaleResolver implements LocaleResolver {

    private final Locale defaultLocale;
    private final List<Locale> supportedLocales;

    public HeaderLocaleResolver(I18nProperties properties) {
        this.defaultLocale = parse(properties.getDefaultLocale());
        this.supportedLocales = properties.getSupportedLocales().stream()
                .map(HeaderLocaleResolver::parse).toList();
    }

    private static Locale parse(String text) {
        String[] parts = text.replace('-', '_').split("_");
        return parts.length > 1 ? Locale.of(parts[0], parts[1]) : Locale.of(parts[0]);
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String header = request.getHeader("Accept-Language");
        if (header == null || header.isBlank()) {
            return defaultLocale;
        }
        try {
            // 按 q 权重排序后逐个匹配，挑出第一个受支持的
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(header);
            Locale matched = Locale.lookup(ranges, supportedLocales);
            return matched != null ? matched : defaultLocale;
        } catch (Exception e) {
            // 头是客户端可控的输入，格式非法不该导致请求失败
            log.debug("[resolveLocale] Accept-Language 解析失败: {}", header);
            return defaultLocale;
        }
    }

    @Override
    public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response,
                          @Nullable Locale locale) {
        throw new UnsupportedOperationException("语言由 Accept-Language 头决定，不支持运行时设置");
    }
}
