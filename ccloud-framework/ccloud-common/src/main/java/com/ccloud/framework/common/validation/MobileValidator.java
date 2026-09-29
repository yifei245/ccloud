package com.ccloud.framework.common.validation;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.util.validation.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MobileValidator implements ConstraintValidator<Mobile, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 空值不校验，交给 @NotBlank / @NotNull 表达必填语义
        if (StrUtil.isEmpty(value)) {
            return true;
        }
        return ValidationUtils.isMobile(value);
    }
}
