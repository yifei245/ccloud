package com.ccloud.module.infra.service.db;

import com.ccloud.framework.test.core.ut.LocalMySql;
import com.ccloud.module.infra.service.db.dto.ColumnInfo;
import com.ccloud.module.infra.service.db.dto.IndexInfo;
import com.ccloud.module.infra.service.db.dto.TableInfo;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import({DatabaseTableServiceImpl.class, DataSourceConfigServiceImpl.class})
class DatabaseTableServiceImplTest extends BaseInfraDbUnitTest {

    @Resource
    private DatabaseTableServiceImpl databaseTableService;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void getTable_读取字段主键自增与索引() {
        TableInfo table = databaseTableService.getTable(0L, "infra_config");

        assertThat(table.name()).isEqualTo("infra_config");
        ColumnInfo id = table.columns().stream().filter(ColumnInfo::primaryKey).findFirst().orElseThrow();
        assertThat(table.columns().stream().filter(ColumnInfo::primaryKey)).hasSize(1);
        assertThat(id.name()).isEqualTo("id");
        assertThat(id.autoIncrement()).isTrue();
        assertThat(id.dataType()).isEqualTo("bigint");

        ColumnInfo key = table.columns().stream().filter(c -> c.name().equals("config_key")).findFirst().orElseThrow();
        assertThat(key.columnType()).isEqualTo("varchar(100)");
        assertThat(key.charMaxLength()).isEqualTo(100L);
        assertThat(key.nullable()).isFalse();
        assertThat(key.primaryKey()).isFalse();
        assertThat(key.autoIncrement()).isFalse();
        assertThat(key.comment()).isEqualTo("参数键名");

        IndexInfo unique = table.indexes().stream().filter(i -> i.name().equals("uk_config_key")).findFirst().orElseThrow();
        assertThat(unique.unique()).isTrue();
        assertThat(unique.columns()).containsExactly("config_key", "deleted");
        IndexInfo primary = table.indexes().stream().filter(i -> i.name().equals("PRIMARY")).findFirst().orElseThrow();
        assertThat(primary.columns()).containsExactly("id");
    }

    @Test
    void getTable_不存在返回null() {
        assertThat(databaseTableService.getTable(0L, "no_such_table")).isNull();
    }

    @Test
    void getTableList_按表名与注释筛选_不含字段() {
        List<TableInfo> byName = databaseTableService.getTableList(0L, "infra_demo", null);
        assertThat(byName).extracting(TableInfo::name).containsExactly("infra_demo_category", "infra_demo_student");
        assertThat(byName).allSatisfy(t -> {
            assertThat(t.columns()).isEmpty();
            assertThat(t.indexes()).isEmpty();
        });

        List<TableInfo> byComment = databaseTableService.getTableList(0L, null, "演示学生");
        assertThat(byComment).extracting(TableInfo::name).containsExactly("infra_demo_student");
    }

    @Test
    void getTableListWithDetails_包含全部表与字段() {
        List<TableInfo> tables = databaseTableService.getTableListWithDetails(0L);

        Long expected = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()", Long.class);
        assertThat(tables).hasSize(expected.intValue());
        TableInfo category = tables.stream().filter(t -> t.name().equals("infra_demo_category")).findFirst().orElseThrow();
        assertThat(category.columns()).hasSize(9);
        assertThat(category.columns()).extracting(ColumnInfo::ordinalPosition).isSorted();
    }

    @Test
    void getDatabaseName_返回当前库() {
        assertThat(databaseTableService.getDatabaseName(0L)).isEqualTo(LocalMySql.database());
    }
}
