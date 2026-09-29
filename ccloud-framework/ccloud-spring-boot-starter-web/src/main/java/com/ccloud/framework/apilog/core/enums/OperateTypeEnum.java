package com.ccloud.framework.apilog.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作类型
 */
@Getter
@AllArgsConstructor
public enum OperateTypeEnum {

    OTHER(0, "其它"),
    GET(1, "查询"),
    CREATE(2, "新增"),
    UPDATE(3, "修改"),
    DELETE(4, "删除"),
    EXPORT(5, "导出"),
    IMPORT(6, "导入");

    private final Integer type;
    private final String name;
}
