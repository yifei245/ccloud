package com.ccloud.module.open.enums;

import com.ccloud.framework.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeConstantsTest {

    private static Map<String, ErrorCode> allErrorCodes() throws IllegalAccessException {
        Map<String, ErrorCode> result = new LinkedHashMap<>();
        for (Field field : ErrorCodeConstants.class.getDeclaredFields()) {
            if (field.getType() == ErrorCode.class && Modifier.isStatic(field.getModifiers())) {
                result.put(field.getName(), (ErrorCode) field.get(null));
            }
        }
        return result;
    }

    @Test
    @DisplayName("应定义开放应用、接口目录、回调错误码")
    void shouldDefineErrorCodes() throws Exception {
        assertThat(allErrorCodes()).isNotEmpty();
    }

    @Test
    @DisplayName("全部错误码不得重复——重复会让前端按码分支时走错逻辑")
    void codesShouldBeUnique() throws Exception {
        List<Integer> codes = allErrorCodes().values().stream().map(ErrorCode::getCode).toList();
        assertThat(codes).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("全部错误码应落在 open 模块段位 1-005-xxx-xxx")
    void codesShouldBeInOpenRange() throws Exception {
        allErrorCodes().forEach((name, ec) ->
                assertThat(ec.getCode() / 1_000_000).as(name).isEqualTo(1_005));
    }

    @Test
    @DisplayName("错误提示不得为空")
    void messagesShouldNotBeBlank() throws Exception {
        allErrorCodes().forEach((name, ec) -> assertThat(ec.getMsg()).as(name).isNotBlank());
    }

    @Test
    @DisplayName("每个错误码都应在 06-i18n.sql 中同时有中文与英文文案")
    void everyCodeShouldHaveBothTranslations() throws Exception {
        Set<String> zh = SqlFixture.i18nCodes("zh_CN");
        Set<String> en = SqlFixture.i18nCodes("en_US");
        List<String> missingZh = new ArrayList<>();
        List<String> missingEn = new ArrayList<>();
        allErrorCodes().forEach((name, ec) -> {
            String key = "error.code." + ec.getCode();
            if (!zh.contains(key)) missingZh.add(name + "(" + key + ")");
            if (!en.contains(key)) missingEn.add(name + "(" + key + ")");
        });
        assertThat(missingZh).as("缺少中文文案").isEmpty();
        assertThat(missingEn).as("缺少英文文案").isEmpty();
    }
}
