package com.ccloud.module.infra.enums.codegen;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 代码生成中，字段的显示类型
 */
@Getter
@AllArgsConstructor
public enum CodegenColumnHtmlTypeEnum {

    INPUT("input"),
    TEXTAREA("textarea"),
    SELECT("select"),
    RADIO("radio"),
    DATETIME("datetime");

    private final String type;
}
