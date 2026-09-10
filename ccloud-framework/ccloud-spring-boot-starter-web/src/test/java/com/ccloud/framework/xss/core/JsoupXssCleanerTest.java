package com.ccloud.framework.xss.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsoupXssCleanerTest {

    private final XssCleaner cleaner = new JsoupXssCleaner();

    @Test
    @DisplayName("应剥离 script 标签")
    void shouldStripScriptTag() {
        assertThat(cleaner.clean("<p>hi</p><script>alert(1)</script>")).doesNotContain("script");
    }

    @Test
    @DisplayName("应剥离事件属性")
    void shouldStripEventHandlerAttribute() {
        assertThat(cleaner.clean("<img src=\"x\" onerror=\"alert(1)\">")).doesNotContain("onerror");
    }

    @Test
    @DisplayName("应剥离 javascript: 协议的链接")
    void shouldStripJavascriptProtocolLink() {
        assertThat(cleaner.clean("<a href=\"javascript:alert(1)\">click</a>")).doesNotContain("javascript:");
    }

    @Test
    @DisplayName("应保留普通富文本标签")
    void shouldKeepBenignRichTextTags() {
        String cleaned = cleaner.clean("<p><strong>bold</strong><em>italic</em></p>");
        assertThat(cleaned).contains("<strong>").contains("<em>");
    }

    @Test
    @DisplayName("应保留 http/https 图片，便于富文本正常展示")
    void shouldKeepHttpImages() {
        assertThat(cleaner.clean("<img src=\"https://cdn.example.com/a.png\">"))
                .contains("https://cdn.example.com/a.png");
    }

    @Test
    @DisplayName("空值与空串应原样返回，不抛异常")
    void shouldPassThroughBlankInput() {
        assertThat(cleaner.clean(null)).isNull();
        assertThat(cleaner.clean("")).isEmpty();
    }

    @Test
    @DisplayName("不应转义中文与常见符号")
    void shouldNotEscapeChineseCharacters() {
        assertThat(cleaner.clean("张三 & 李四")).contains("张三").contains("李四");
    }
}
