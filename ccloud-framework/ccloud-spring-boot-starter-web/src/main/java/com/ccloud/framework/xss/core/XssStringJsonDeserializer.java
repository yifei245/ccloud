package com.ccloud.framework.xss.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

/**
 * 反序列化 JSON 字符串字段时顺带做 XSS 清洗。
 *
 * <p>放在反序列化环节而非过滤器：此时已经完成 JSON 解析，
 * 能精确地只清洗字符串字段，不会破坏数字、布尔等其他类型。
 */
@RequiredArgsConstructor
public class XssStringJsonDeserializer extends JsonDeserializer<String> {

    private final XssCleaner xssCleaner;

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return xssCleaner.clean(p.getValueAsString());
    }
}
