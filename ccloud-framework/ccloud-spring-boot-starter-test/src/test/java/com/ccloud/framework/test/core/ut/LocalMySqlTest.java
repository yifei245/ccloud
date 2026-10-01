package com.ccloud.framework.test.core.ut;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalMySqlTest {

    @Test
    void ddlTableNames_包含DDL中的表且不含已删除的表() {
        assertThat(LocalMySql.ddlTableNames())
                .contains("infra_config", "infra_demo_student")
                .doesNotContain("infra_job");
    }

    @Test
    void 初始化时删除DDL中不存在的表() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                LocalMySql.jdbcUrl(), LocalMySql.username(), LocalMySql.password());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS ut_stale_table (id bigint primary key)");
            assertThat(tableExists(connection, "ut_stale_table")).isTrue();

            LocalMySql.dropTablesNotInDdl(connection);

            assertThat(tableExists(connection, "ut_stale_table")).isFalse();
            assertThat(tableExists(connection, "infra_config")).isTrue();
        }
    }

    @Test
    void 业务库名被拒绝() {
        assertThatThrownBy(() -> LocalMySql.assertNotBusinessDatabase("ccloud"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("测试库不能是业务库 ccloud");
        assertThatCode(() -> LocalMySql.assertNotBusinessDatabase("ccloud_test")).doesNotThrowAnyException();
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM information_schema.TABLES "
                     + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + table + "'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
}
