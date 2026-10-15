package com.ccloud.module.pay.controller.open.json;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 开放接口的响应时间统一输出毫秒时间戳（系统时区），与开放平台回调推送的时间格式一致。
 * 只用于开放接口的响应字段（字段级注解），不改全局 Jackson 配置
 */
public class OpenEpochMilliSerializer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeNumber(LocalDateTimeUtil.toEpochMilli(value));
    }
}
