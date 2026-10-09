package com.ccloud.framework.test.core.ut;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

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
    void 库名不以_test结尾时跳过删表_不抛异常() throws SQLException {
        // 误配到本机共享 MySQL 中其他项目的库时，删表会清空该项目的数据且不可恢复：一条语句都不能执行
        LocalMySql.dropTablesNotInDdl(connectionOf("other_project"), Set.of("infra_config"));
        LocalMySql.dropTablesNotInDdl(connectionOf("ccloud_test_backup"), Set.of("infra_config"));
        assertThatThrownBy(() -> LocalMySql.dropTablesNotInDdl(connectionOf("ccloud"), Set.of("infra_config")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("测试库不能是业务库 ccloud");
    }

    @Test
    void DDL表名为空时拒绝删表() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                LocalMySql.jdbcUrl(), LocalMySql.username(), LocalMySql.password());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS ut_stale_table (id bigint primary key)");

            assertThatThrownBy(() -> LocalMySql.dropTablesNotInDdl(connection, Set.of()))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(tableExists(connection, "ut_stale_table")).isTrue();
            assertThat(tableExists(connection, "infra_config")).isTrue();
            statement.execute("DROP TABLE ut_stale_table");
        }
    }

    /**
     * 只回答库名的连接：执行任何语句都直接失败，用来证明没有发出删表语句
     */
    private static Connection connectionOf(String database) {
        return (Connection) Proxy.newProxyInstance(LocalMySqlTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getCatalog")) {
                        return database;
                    }
                    throw new AssertionError("不应调用 Connection#" + method.getName());
                });
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
