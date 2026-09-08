package com.ccloud.framework.common.util.json;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * JSON 工具。全项目共用一个 ObjectMapper，序列化规则集中在这里：
 * <ul>
 *   <li>Long / BigInteger 序列化为字符串——JS 的 Number 只有 53 位精度，
 *       雪花 ID 直接传数字会被截断；</li>
 *   <li>时间统一 {@code yyyy-MM-dd HH:mm:ss}；</li>
 *   <li>反序列化遇到未知字段不报错，方便接口平滑升级。</li>
 * </ul>
 */
@Slf4j
public class JsonUtils {

    public static final String DEFAULT_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    public static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    /**
     * 构建与全项目一致的 ObjectMapper。Spring 容器里的 Jackson 配置也复用它。
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_PATTERN)));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_PATTERN)));
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN)));
        javaTimeModule.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN)));
        javaTimeModule.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN)));
        mapper.registerModule(javaTimeModule);

        // 长整型转字符串，避免前端精度丢失
        SimpleModule longModule = new SimpleModule();
        longModule.addSerializer(Long.class, ToStringSerializer.instance);
        longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        longModule.addSerializer(java.math.BigInteger.class, ToStringSerializer.instance);
        mapper.registerModule(longModule);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        return mapper;
    }

    public static String toJsonString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 序列化失败: " + object.getClass().getName(), e);
        }
    }

    public static byte[] toJsonByte(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 序列化失败: " + object.getClass().getName(), e);
        }
    }

    public static String toJsonPrettyString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 序列化失败: " + object.getClass().getName(), e);
        }
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(text, clazz);
        } catch (JsonProcessingException e) {
            log.error("[parseObject] 反序列化失败，目标类型 {}，内容 {}", clazz.getName(), text, e);
            throw new IllegalArgumentException("JSON 反序列化失败", e);
        }
    }

    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (ArrayUtil.isEmpty(bytes)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(bytes, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 反序列化失败", e);
        }
    }

    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(text, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 反序列化失败", e);
        }
    }

    /**
     * 反序列化失败时返回 null 而非抛异常。用于读取历史数据等"失败可容忍"的场景。
     */
    public static <T> T parseObjectQuietly(String text, TypeReference<T> typeReference) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(text, typeReference);
        } catch (Exception e) {
            log.warn("[parseObjectQuietly] 反序列化失败，已忽略。内容 {}", text);
            return null;
        }
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return List.of();
        }
        try {
            JavaType type = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
            return OBJECT_MAPPER.readValue(text, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 反序列化失败", e);
        }
    }

    public static boolean isJson(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(text);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
