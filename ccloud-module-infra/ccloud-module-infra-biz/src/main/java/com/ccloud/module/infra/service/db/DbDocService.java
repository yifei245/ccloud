package com.ccloud.module.infra.service.db;

/**
 * 数据库文档：把数据源当前库的表结构导出为文档
 */
public interface DbDocService {

    /**
     * 导出 HTML 文档，所有来自数据库的文本均已转义
     */
    String exportHtml(Long dataSourceConfigId);

    /**
     * 导出 Markdown 文档，表格单元格中的竖线与换行已转义
     */
    String exportMarkdown(Long dataSourceConfigId);
}
