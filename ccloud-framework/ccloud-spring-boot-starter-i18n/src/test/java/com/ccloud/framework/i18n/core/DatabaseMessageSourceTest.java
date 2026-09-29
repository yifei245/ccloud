package com.ccloud.framework.i18n.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMessageSourceTest {

    private DatabaseMessageSource messageSource;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource parent = new ResourceBundleMessageSource();
        parent.setBasename("i18n/messages");
        parent.setDefaultEncoding("UTF-8");
        parent.setUseCodeAsDefaultMessage(true);

        messageSource = new DatabaseMessageSource(parent);
        messageSource.refresh("zh_CN", Map.of("demo.db.only", "来自数据库的文案",
                "demo.override", "数据库覆盖版"));
        messageSource.refresh("en_US", Map.of("demo.db.only", "From database"));
    }

    @Test
    @DisplayName("数据库中存在时优先返回数据库文案，使文案可在后台随时调整")
    void shouldPreferDatabaseMessage() {
        assertThat(messageSource.getMessage("demo.db.only", null, Locale.CHINA))
                .isEqualTo("来自数据库的文案");
    }

    @Test
    @DisplayName("数据库中不存在时回退到 properties 基线")
    void shouldFallbackToPropertiesBaseline() {
        assertThat(messageSource.getMessage("error.internal", null, Locale.CHINA))
                .isEqualTo("系统异常");
    }

    @Test
    @DisplayName("两处都不存在时返回 code 本身，不抛异常中断业务")
    void shouldReturnCodeWhenMissingEverywhere() {
        assertThat(messageSource.getMessage("not.exist.at.all", null, Locale.CHINA))
                .isEqualTo("not.exist.at.all");
    }

    @Test
    @DisplayName("不同语言应各自取各自的文案")
    void shouldResolvePerLocale() {
        assertThat(messageSource.getMessage("demo.db.only", null, Locale.US)).isEqualTo("From database");
        assertThat(messageSource.getMessage("demo.db.only", null, Locale.CHINA)).isEqualTo("来自数据库的文案");
    }

    @Test
    @DisplayName("占位符应被参数替换")
    void shouldFormatPlaceholders() {
        messageSource.refresh("zh_CN", Map.of("demo.with.param", "用户 {0} 不存在"));
        assertThat(messageSource.getMessage("demo.with.param", new Object[]{"张三"}, Locale.CHINA))
                .isEqualTo("用户 张三 不存在");
    }

    @Test
    @DisplayName("refresh 应整体替换该语言的文案，删除的条目不再命中")
    void refreshShouldReplaceWholeLocale() {
        messageSource.refresh("zh_CN", Map.of("only.this", "唯一"));
        assertThat(messageSource.getMessage("demo.db.only", null, Locale.CHINA))
                .isEqualTo("demo.db.only");
        assertThat(messageSource.getMessage("only.this", null, Locale.CHINA)).isEqualTo("唯一");
    }
}
