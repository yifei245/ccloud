package com.ccloud.framework.test.core.ut;

import com.ccloud.framework.mybatis.config.CcloudMybatisAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据库单测基类。
 *
 * <p>用 H2 的 MySQL 兼容模式而非真实 MySQL：测试不依赖外部服务，
 * CI 上无需先起容器。代价是 MySQL 特有语法（如 ON DUPLICATE KEY）无法覆盖，
 * 这类 SQL 需要额外的集成测试。
 *
 * <p>每个测试方法运行在独立事务中并在结束后回滚，测试之间不会互相污染。
 */
@SpringBootTest(classes = BaseDbUnitTest.Application.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@ImportAutoConfiguration(classes = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        CcloudMybatisAutoConfiguration.class
})
public abstract class BaseDbUnitTest {

    @org.springframework.boot.autoconfigure.SpringBootApplication
    public static class Application {
    }
}
