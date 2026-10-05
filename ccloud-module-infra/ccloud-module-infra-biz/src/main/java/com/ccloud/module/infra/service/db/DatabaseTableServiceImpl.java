package com.ccloud.module.infra.service.db;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.module.infra.service.db.dto.ColumnInfo;
import com.ccloud.module.infra.service.db.dto.IndexInfo;
import com.ccloud.module.infra.service.db.dto.TableInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.DATA_SOURCE_CONFIG_NOT_OK;

@Slf4j
@Service
public class DatabaseTableServiceImpl implements DatabaseTableService {

    private static final String SQL_TABLES = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES "
            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME";
    private static final String SQL_COLUMNS = "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, DATA_TYPE, COLUMN_COMMENT, "
            + "IS_NULLABLE, COLUMN_DEFAULT, COLUMN_KEY, EXTRA, ORDINAL_POSITION, CHARACTER_MAXIMUM_LENGTH "
            + "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() %s ORDER BY TABLE_NAME, ORDINAL_POSITION";
    private static final String SQL_INDEXES = "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, COLUMN_NAME "
            + "FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() %s ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX";

    @Resource
    private DataSourceConfigService dataSourceConfigService;

    @Override
    public List<TableInfo> getTableList(Long dataSourceConfigId, String name, String comment) {
        return execute(dataSourceConfigId, conn -> queryTables(conn).stream()
                .filter(t -> StrUtil.isEmpty(name) || StrUtil.containsIgnoreCase(t.name(), name))
                .filter(t -> StrUtil.isEmpty(comment) || StrUtil.containsIgnoreCase(t.comment(), comment))
                .toList());
    }

    @Override
    public TableInfo getTable(Long dataSourceConfigId, String tableName) {
        return execute(dataSourceConfigId, conn -> {
            TableInfo table = CollUtil.findOne(queryTables(conn), t -> t.name().equals(tableName));
            if (table == null) {
                return null;
            }
            return withDetails(List.of(table), queryColumns(conn, tableName), queryIndexes(conn, tableName)).get(0);
        });
    }

    @Override
    public List<TableInfo> getTableListWithDetails(Long dataSourceConfigId) {
        return execute(dataSourceConfigId, conn ->
                withDetails(queryTables(conn), queryColumns(conn, null), queryIndexes(conn, null)));
    }

    @Override
    public String getDatabaseName(Long dataSourceConfigId) {
        return execute(dataSourceConfigId, conn -> {
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
                rs.next();
                return rs.getString(1);
            }
        });
    }

    private static List<TableInfo> queryTables(Connection conn) throws SQLException {
        List<TableInfo> tables = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(SQL_TABLES)) {
            while (rs.next()) {
                tables.add(new TableInfo(rs.getString("TABLE_NAME"), StrUtil.nullToEmpty(rs.getString("TABLE_COMMENT")),
                        List.of(), List.of()));
            }
        }
        return tables;
    }

    /**
     * @param tableName 为空查全部表
     * @return 表名 → 字段，按字段顺序
     */
    private static Map<String, List<ColumnInfo>> queryColumns(Connection conn, String tableName) throws SQLException {
        Map<String, List<ColumnInfo>> result = new LinkedHashMap<>();
        try (PreparedStatement ps = prepare(conn, SQL_COLUMNS, tableName); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // 非字符类型为 NULL，须按对象读取，不能用 getLong（会得到 0）
                Long charMaxLength = rs.getObject("CHARACTER_MAXIMUM_LENGTH", Long.class);
                ColumnInfo column = new ColumnInfo(rs.getString("COLUMN_NAME"), rs.getString("COLUMN_TYPE"),
                        rs.getString("DATA_TYPE").toLowerCase(Locale.ROOT), StrUtil.nullToEmpty(rs.getString("COLUMN_COMMENT")),
                        "YES".equals(rs.getString("IS_NULLABLE")), rs.getString("COLUMN_DEFAULT"),
                        "PRI".equals(rs.getString("COLUMN_KEY")),
                        StrUtil.containsIgnoreCase(rs.getString("EXTRA"), "auto_increment"),
                        rs.getInt("ORDINAL_POSITION"), charMaxLength);
                result.computeIfAbsent(rs.getString("TABLE_NAME"), k -> new ArrayList<>()).add(column);
            }
        }
        return result;
    }

    /**
     * @param tableName 为空查全部表
     * @return 表名 → 索引，同一索引的多个字段聚合在一起
     */
    private static Map<String, List<IndexInfo>> queryIndexes(Connection conn, String tableName) throws SQLException {
        // 表名 → 索引名 → 索引，保持查询的排序
        Map<String, Map<String, IndexBuilder>> grouped = new LinkedHashMap<>();
        try (PreparedStatement ps = prepare(conn, SQL_INDEXES, tableName); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                boolean unique = rs.getInt("NON_UNIQUE") == 0;
                grouped.computeIfAbsent(rs.getString("TABLE_NAME"), k -> new LinkedHashMap<>())
                        .computeIfAbsent(indexName, k -> new IndexBuilder(k, unique))
                        .columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        Map<String, List<IndexInfo>> result = new LinkedHashMap<>();
        grouped.forEach((table, indexes) -> result.put(table,
                indexes.values().stream().map(b -> new IndexInfo(b.name, b.unique, List.copyOf(b.columns))).toList()));
        return result;
    }

    private static PreparedStatement prepare(Connection conn, String sqlTemplate, String tableName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                String.format(sqlTemplate, StrUtil.isEmpty(tableName) ? "" : "AND TABLE_NAME = ?"));
        if (StrUtil.isNotEmpty(tableName)) {
            ps.setString(1, tableName);
        }
        return ps;
    }

    private static List<TableInfo> withDetails(List<TableInfo> tables, Map<String, List<ColumnInfo>> columns,
                                               Map<String, List<IndexInfo>> indexes) {
        return tables.stream().map(t -> new TableInfo(t.name(), t.comment(),
                columns.getOrDefault(t.name(), List.of()), indexes.getOrDefault(t.name(), List.of()))).toList();
    }

    private <T> T execute(Long dataSourceConfigId, SqlFunction<Connection, T> action) {
        try (Connection conn = dataSourceConfigService.openConnection(dataSourceConfigId)) {
            return action.apply(conn);
        } catch (SQLException e) {
            log.warn("[execute][读取数据源({})的表结构失败]", dataSourceConfigId, e);
            throw exception(DATA_SOURCE_CONFIG_NOT_OK);
        }
    }

    private record IndexBuilder(String name, boolean unique, List<String> columns) {
        IndexBuilder(String name, boolean unique) {
            this(name, unique, new ArrayList<>());
        }
    }

    @FunctionalInterface
    private interface SqlFunction<A, R> {
        R apply(A a) throws SQLException;
    }
}
