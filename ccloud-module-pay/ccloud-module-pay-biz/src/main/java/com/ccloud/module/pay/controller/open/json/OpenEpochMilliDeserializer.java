package com.ccloud.module.pay.controller.open.json;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 开放接口的请求时间只接受毫秒时间戳（整数），精度为秒：毫秒部分忽略。
 * ISO 字符串、带引号的数字、数组、小数、超出范围的数一律按请求体格式错误处理（400），
 * 避免同一字段在不同调用方手里有多种写法，也避免过大的数在转换时变成 500。
 * 合法范围为 0 到 2099-12-31 23:59:59。只用于开放接口的请求字段（字段级注解），不改全局 Jackson 配置
 */
public class OpenEpochMilliDeserializer extends JsonDeserializer<LocalDateTime> {

    /** 可接受的最大时间 */
    public static final LocalDateTime MAX_TIME = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
    private static final long MAX_MILLIS = LocalDateTimeUtil.toEpochMilli(MAX_TIME);

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            return (LocalDateTime) context.handleUnexpectedToken(LocalDateTime.class, parser);
        }
        // 超过 long 范围的整数无法取值，同样按格式错误处理
        if (parser.getNumberType() == JsonParser.NumberType.BIG_INTEGER) {
            return (LocalDateTime) context.handleWeirdNumberValue(LocalDateTime.class, parser.getBigIntegerValue(),
                    "时间戳超出范围");
        }
        long millis = parser.getLongValue();
        if (millis < 0 || millis > MAX_MILLIS) {
            return (LocalDateTime) context.handleWeirdNumberValue(LocalDateTime.class, millis,
                    "时间戳须在 0 到 2099-12-31 23:59:59 之间");
        }
        return LocalDateTimeUtil.of(millis).withNano(0);
    }
}
