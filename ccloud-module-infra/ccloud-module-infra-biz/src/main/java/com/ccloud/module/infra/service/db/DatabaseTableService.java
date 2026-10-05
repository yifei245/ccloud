package com.ccloud.module.infra.service.db;

import com.ccloud.module.infra.service.db.dto.TableInfo;

import java.util.List;

/**
 * 读取数据源当前库的表结构（information_schema）
 */
public interface DatabaseTableService {

    /**
     * 表清单，按表名、表注释模糊筛选（忽略大小写，为空不筛选）；不含字段与索引
     */
    List<TableInfo> getTableList(Long dataSourceConfigId, String name, String comment);

    /**
     * 单表结构，含字段与索引；不存在返回 null
     */
    TableInfo getTable(Long dataSourceConfigId, String tableName);

    /**
     * 全部表，含字段与索引
     */
    List<TableInfo> getTableListWithDetails(Long dataSourceConfigId);

    /**
     * 当前库名
     */
    String getDatabaseName(Long dataSourceConfigId);
}
