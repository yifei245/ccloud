package com.ccloud.module.infra.enums.codegen;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 代码生成的模板类型
 */
@Getter
@AllArgsConstructor
public enum CodegenTemplateTypeEnum {

    /**
     * 单表
     */
    ONE(1),
    /**
     * 树表
     */
    TREE(2);

    private final Integer type;

    /**
     * @return 类型不存在返回 null
     */
    public static CodegenTemplateTypeEnum valueOf(Integer type) {
        return Arrays.stream(values()).filter(e -> e.getType().equals(type)).findFirst().orElse(null);
    }
}
