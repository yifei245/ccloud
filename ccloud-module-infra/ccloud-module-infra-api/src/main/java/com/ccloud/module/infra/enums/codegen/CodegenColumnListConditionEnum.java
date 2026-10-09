package com.ccloud.module.infra.enums.codegen;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 代码生成中，字段作为查询条件时的比较方式
 */
@Getter
@AllArgsConstructor
public enum CodegenColumnListConditionEnum {

    EQ("=", "eqIfPresent"),
    NE("!=", "neIfPresent"),
    GT(">", "gtIfPresent"),
    GTE(">=", "geIfPresent"),
    LT("<", "ltIfPresent"),
    LTE("<=", "leIfPresent"),
    LIKE("LIKE", "likeIfPresent"),
    BETWEEN("BETWEEN", "betweenIfPresent");

    /**
     * 条件
     */
    private final String condition;
    /**
     * 对应 LambdaQueryWrapperX 的方法名
     */
    private final String queryMethod;

    /**
     * @return 条件不存在返回 null
     */
    public static CodegenColumnListConditionEnum valueOfCondition(String condition) {
        return Arrays.stream(values()).filter(e -> e.getCondition().equals(condition)).findFirst().orElse(null);
    }
}
