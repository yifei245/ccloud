package com.ccloud.module.infra.framework.file.core.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

public class FileTypeUtilsTest {

    /** 1x1 透明 PNG，其他测试也用它作为合法图片 */
    public static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Test
    void 按内容识别_不信任扩展名() {
        assertThat(FileTypeUtils.detect(PNG, "a.png")).isEqualTo(new FileTypeUtils.FileType("image/png", "png"));
        assertThat(FileTypeUtils.detect(PNG, "a.txt")).isEqualTo(new FileTypeUtils.FileType("image/png", "png"));
        assertThat(FileTypeUtils.detect(PNG, null).extension()).isEqualTo("png");
        assertThat(FileTypeUtils.detect("hello".getBytes(StandardCharsets.UTF_8), "a.txt").extension()).isEqualTo("txt");
        assertThat(FileTypeUtils.detect("a,b\n1,2".getBytes(StandardCharsets.UTF_8), "a.csv").extension()).isEqualTo("csv");
    }

    @Test
    void 网页_SVG_XML_脚本一律拒绝_即使改了扩展名() {
        byte[] html = "<!DOCTYPE html><html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);
        byte[] xml = "<?xml version=\"1.0\"?><a/>".getBytes(StandardCharsets.UTF_8);
        assertThat(FileTypeUtils.detect(html, "a.png")).isNull();
        assertThat(FileTypeUtils.detect(html, "a.txt")).isNull();
        assertThat(FileTypeUtils.detect(svg, "a.svg")).isNull();
        assertThat(FileTypeUtils.detect(svg, "a.png")).isNull();
        assertThat(FileTypeUtils.detect(xml, "a.xml")).isNull();
        assertThat(FileTypeUtils.detect("#!/bin/sh\nrm -rf /".getBytes(StandardCharsets.UTF_8), "a.sh")).isNull();
        assertThat(FileTypeUtils.detect(new byte[]{0x7f, 'E', 'L', 'F', 2, 1, 1, 0}, "a.png")).isNull();
    }

    @Test
    void 图片与PDF可在浏览器内打开_其他作为附件() {
        assertThat(FileTypeUtils.isInline("image/png")).isTrue();
        assertThat(FileTypeUtils.isInline("application/pdf")).isTrue();
        assertThat(FileTypeUtils.isInline("text/plain")).isFalse();
        assertThat(FileTypeUtils.isInline("application/zip")).isFalse();
    }
}
