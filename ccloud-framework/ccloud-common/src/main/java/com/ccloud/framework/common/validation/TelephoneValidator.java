package com.ccloud.framework.common.validation;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.util.validation.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TelephoneValidator implements ConstraintValidator<Telephone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isEmpty(value)) {
            return true;
        }
        return ValidationUtils.isTelephone(value);
    }
}
