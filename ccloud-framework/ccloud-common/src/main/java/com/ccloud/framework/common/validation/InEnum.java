package com.ccloud.framework.common.validation;

import com.ccloud.framework.common.core.IntArrayValuable;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验值必须落在指定枚举的合法取值内。
 *
 * <p>比 {@code @Range} 更贴切：新增枚举项时校验规则自动跟随，不用改注解参数。
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR,
        ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {InEnumValidator.class, InEnumCollectionValidator.class})
public @interface InEnum {

    /**
     * 实现了 {@link IntArrayValuable} 的枚举类
     */
    Class<? extends IntArrayValuable> value();

    String message() default "必须在指定范围 {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
