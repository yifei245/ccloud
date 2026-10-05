package com.ccloud.module.infra.service.db.dto;

import java.util.List;

/**
 * 数据库索引
 *
 * @param columns 索引包含的字段，按索引内顺序
 */
public record IndexInfo(String name, boolean unique, List<String> columns) {
}
