package com.ccloud.framework.common.validation;

import com.ccloud.framework.common.core.IntArrayValuable;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * {@link InEnum} 作用于集合字段时的校验器，要求集合内每个元素都合法
 */
public class InEnumCollectionValidator implements ConstraintValidator<InEnum, Collection<Integer>> {

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
    public boolean isValid(Collection<Integer> list, ConstraintValidatorContext context) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        if (values.containsAll(list)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                context.getDefaultConstraintMessageTemplate().replace("{value}", values.toString())
        ).addConstraintViolation();
        return false;
    }
}
