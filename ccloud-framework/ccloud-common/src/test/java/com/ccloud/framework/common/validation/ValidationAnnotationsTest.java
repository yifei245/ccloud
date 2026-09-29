package com.ccloud.framework.common.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationAnnotationsTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @AllArgsConstructor
    static class MobileForm {
        @Mobile
        private String mobile;
    }

    @AllArgsConstructor
    static class StatusForm {
        @InEnum(TestStatusEnum.class)
        private Integer status;
    }

    @Test
    @DisplayName("合法手机号应通过校验")
    void mobile_shouldAcceptValidNumber() {
        assertThat(validator.validate(new MobileForm("13800138000"))).isEmpty();
    }

    @Test
    @DisplayName("非法手机号应被拒绝")
    void mobile_shouldRejectInvalidNumber() {
        assertThat(validator.validate(new MobileForm("1380013"))).hasSize(1);
        assertThat(validator.validate(new MobileForm("23800138000"))).hasSize(1);
        assertThat(validator.validate(new MobileForm("abcdefghijk"))).hasSize(1);
    }

    @Test
    @DisplayName("手机号为空时跳过校验，是否必填交给 @NotBlank")
    void mobile_shouldSkipWhenBlank() {
        assertThat(validator.validate(new MobileForm(null))).isEmpty();
        assertThat(validator.validate(new MobileForm(""))).isEmpty();
    }

    @Test
    @DisplayName("InEnum 应接受枚举内的值、拒绝枚举外的值")
    void inEnum_shouldAcceptOnlyDefinedValues() {
        assertThat(validator.validate(new StatusForm(0))).isEmpty();
        assertThat(validator.validate(new StatusForm(1))).isEmpty();
        assertThat(validator.validate(new StatusForm(9))).hasSize(1);
    }

    @Test
    @DisplayName("InEnum 校验失败时提示语应列出全部合法取值")
    void inEnum_messageShouldListAllowedValues() {
        var violations = validator.validate(new StatusForm(9));
        assertThat(violations).first()
                .extracting(v -> v.getMessage().toString())
                .asString().contains("0").contains("1");
    }
}
