package com.ccloud.framework.common.validation;

import com.ccloud.framework.common.core.IntArrayValuable;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

public class InEnumValidator implements ConstraintValidator<InEnum, Integer> {

    private List<Integer> values;

    @Override
    public void initialize(InEnum annotation) {
        IntArrayValuable[] constants = annotation.value().getEnumConstants();
        if (constants == null || constants.length == 0) {
            this.values = List.of();
            return;
        }
        this.values = Arrays.stream(constants[0].array()).boxed().toList();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (values.contains(value)) {
            return true;
        }
        // 把合法取值列进提示语，前端联调时能直接看出问题
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                context.getDefaultConstraintMessageTemplate().replace("{value}", values.toString())
        ).addConstraintViolation();
        return false;
    }
}
