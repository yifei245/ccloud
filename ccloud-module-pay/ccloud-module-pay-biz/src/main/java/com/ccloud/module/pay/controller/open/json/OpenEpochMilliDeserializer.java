package com.ccloud.module.pay.controller.open.json;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 开放接口的请求时间只接受毫秒时间戳（整数，系统时区）。ISO 字符串、带引号的数字、数组、小数一律按请求体格式错误处理，
 * 避免同一字段在不同调用方手里有多种写法。只用于开放接口的请求字段（字段级注解），不改全局 Jackson 配置
 */
public class OpenEpochMilliDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            return (LocalDateTime) context.handleUnexpectedToken(LocalDateTime.class, parser);
        }
        return LocalDateTimeUtil.of(parser.getLongValue());
    }
}
