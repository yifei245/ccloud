package com.ccloud.framework.xss.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;

/**
 * 基于 Jsoup 的 XSS 清洗器。
 *
 * <p>采用白名单策略：只保留明确允许的标签与属性，其余一律剥离。
 * 相比黑名单，白名单不会因为出现新的攻击向量而失效。
 */
public class JsoupXssCleaner implements XssCleaner {

    private final Safelist safelist;

    /**
     * 输出设置：不转义中文等非 ASCII 字符，且不做美化换行，
     * 避免清洗后内容与用户输入产生视觉差异。
     */
    private final Document.OutputSettings outputSettings =
            new Document.OutputSettings().prettyPrint(false).escapeMode(Entities.EscapeMode.xhtml);

    public JsoupXssCleaner() {
        this.safelist = buildSafelist();
    }

    public JsoupXssCleaner(Safelist safelist) {
        this.safelist = safelist;
    }

    /**
     * 在 Jsoup 的 relaxed 白名单基础上放宽两点：
     * 允许保留常见的样式类与 id（前端富文本要用），允许 data 协议的图片。
     */
    private static Safelist buildSafelist() {
        return Safelist.relaxed()
                .addAttributes(":all", "style", "class", "id", "name")
                .addProtocols("img", "src", "http", "https", "data")
                .addAttributes("a", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto", "tel");
    }

    @Override
    public String clean(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, "", safelist, outputSettings);
    }
}
