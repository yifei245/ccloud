package com.ccloud.module.infra.service.db.dto;

import java.util.List;

/**
 * 数据库表结构
 *
 * @param name    表名
 * @param comment 表注释
 * @param columns 字段，按定义顺序；只查表清单时为空列表
 * @param indexes 索引；只查表清单时为空列表
 */
public record TableInfo(String name, String comment, List<ColumnInfo> columns, List<IndexInfo> indexes) {
}
