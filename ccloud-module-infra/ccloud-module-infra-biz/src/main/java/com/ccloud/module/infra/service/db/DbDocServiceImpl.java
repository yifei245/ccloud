package com.ccloud.module.infra.service.db;

import com.ccloud.module.infra.service.db.dto.ColumnInfo;
import com.ccloud.module.infra.service.db.dto.IndexInfo;
import com.ccloud.module.infra.service.db.dto.TableInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DbDocServiceImpl implements DbDocService {

    @Resource
    private DatabaseTableService databaseTableService;

    @Override
    public String exportHtml(Long dataSourceConfigId) {
        String database = databaseTableService.getDatabaseName(dataSourceConfigId);
        List<TableInfo> tables = databaseTableService.getTableListWithDetails(dataSourceConfigId);
        StringBuilder html = new StringBuilder("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"><title>")
                .append(h(database)).append(" 数据库文档</title><style>")
                .append("body{font-family:sans-serif;margin:24px}table{border-collapse:collapse;margin:8px 0 24px}")
                .append("th,td{border:1px solid #ccc;padding:4px 8px;text-align:left}th{background:#f5f5f5}")
                .append("</style></head><body><h1>").append(h(database)).append(" 数据库文档</h1><h2>目录</h2><ol>");
        for (TableInfo table : tables) {
            html.append("<li><a href=\"#").append(h(table.name())).append("\">").append(h(table.name()))
                    .append("</a> ").append(h(table.comment())).append("</li>");
        }
        html.append("</ol>");
        for (TableInfo table : tables) {
            html.append("<h2 id=\"").append(h(table.name())).append("\">").append(h(table.name())).append("</h2><p>")
                    .append(h(table.comment())).append("</p><table><tr><th>序号</th><th>字段</th><th>类型</th>")
                    .append("<th>可空</th><th>默认值</th><th>主键</th><th>注释</th></tr>");
            for (ColumnInfo c : table.columns()) {
                html.append("<tr><td>").append(c.ordinalPosition()).append("</td><td>").append(h(c.name()))
                        .append("</td><td>").append(h(c.columnType())).append("</td><td>").append(c.nullable() ? "是" : "否")
                        .append("</td><td>").append(h(c.defaultValue())).append("</td><td>").append(c.primaryKey() ? "是" : "")
                        .append("</td><td>").append(h(c.comment())).append("</td></tr>");
            }
            html.append("</table>");
            if (!table.indexes().isEmpty()) {
                html.append("<table><tr><th>索引</th><th>唯一</th><th>字段</th></tr>");
                for (IndexInfo index : table.indexes()) {
                    html.append("<tr><td>").append(h(index.name())).append("</td><td>").append(index.unique() ? "是" : "否")
                            .append("</td><td>").append(h(String.join(", ", index.columns()))).append("</td></tr>");
                }
                html.append("</table>");
            }
        }
        return html.append("</body></html>").toString();
    }

    @Override
    public String exportMarkdown(Long dataSourceConfigId) {
        String database = databaseTableService.getDatabaseName(dataSourceConfigId);
        List<TableInfo> tables = databaseTableService.getTableListWithDetails(dataSourceConfigId);
        StringBuilder md = new StringBuilder("# ").append(m(database)).append(" 数据库文档\n\n## 目录\n\n");
        for (TableInfo table : tables) {
            md.append("- [").append(m(table.name())).append("](#").append(m(table.name())).append(") ")
                    .append(m(table.comment())).append('\n');
        }
        for (TableInfo table : tables) {
            md.append("\n## ").append(m(table.name())).append("\n\n").append(m(table.comment())).append("\n\n")
                    .append("| 序号 | 字段 | 类型 | 可空 | 默认值 | 主键 | 注释 |\n")
                    .append("| --- | --- | --- | --- | --- | --- | --- |\n");
            for (ColumnInfo c : table.columns()) {
                md.append("| ").append(c.ordinalPosition()).append(" | ").append(m(c.name()))
                        .append(" | ").append(m(c.columnType())).append(" | ").append(c.nullable() ? "是" : "否")
                        .append(" | ").append(m(c.defaultValue())).append(" | ").append(c.primaryKey() ? "是" : "")
                        .append(" | ").append(m(c.comment())).append(" |\n");
            }
            if (!table.indexes().isEmpty()) {
                md.append("\n| 索引 | 唯一 | 字段 |\n| --- | --- | --- |\n");
                for (IndexInfo index : table.indexes()) {
                    md.append("| ").append(m(index.name())).append(" | ").append(index.unique() ? "是" : "否")
                            .append(" | ").append(m(String.join(", ", index.columns()))).append(" |\n");
                }
            }
        }
        return md.toString();
    }

    private static String h(String text) {
        return text == null ? "" : HtmlUtils.htmlEscape(text, StandardCharsets.UTF_8.name());
    }

    /**
     * Markdown 文本：先 HTML 转义，再转义表格分隔符与换行
     */
    private static String m(String text) {
        return h(text).replace("|", "\\|").replace("\r\n", "<br>").replace("\n", "<br>").replace("\r", "<br>");
    }
}
