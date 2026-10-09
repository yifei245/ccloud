package com.ccloud.framework.test.core.ut;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BaseDbUnitTestTest extends BaseDbUnitTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Order(1)
    void 应加载仓库正式DDL中的全部表() {
        // 连的是本机 MySQL 上的测试专用库，不是正式库 ccloud
        assertThat(jdbcTemplate.queryForObject("SELECT DATABASE()", String.class))
                .isEqualTo(LocalMySql.database()).isNotEqualTo("ccloud");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()", Integer.class);
        assertThat(count).isEqualTo(143);
    }

    @Test
    @Order(2)
    void 测试内写入的数据() {
        jdbcTemplate.update("INSERT INTO system_post (code, name, sort, status, tenant_id) VALUES ('ut', '中文岗位', 1, 0, 1)");
        assertThat(jdbcTemplate.queryForObject("SELECT name FROM system_post WHERE code = 'ut'", String.class))
                .isEqualTo("中文岗位");
    }

    @Test
    @Order(3)
    void 应在测试结束后回滚() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_post WHERE code = 'ut'", Integer.class))
                .isZero();
    }
}
