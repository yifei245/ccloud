package com.ccloud.module.infra.service.db.dto;

/**
 * 数据库字段
 *
 * @param columnType    完整类型，如 varchar(64)、bigint、decimal(10,2)
 * @param dataType      基础类型，小写，如 varchar、bigint
 * @param charMaxLength 字符类型的最大长度，其他类型为 null
 */
public record ColumnInfo(String name, String columnType, String dataType, String comment, boolean nullable,
                         String defaultValue, boolean primaryKey, boolean autoIncrement, int ordinalPosition,
                         Long charMaxLength) {
}
