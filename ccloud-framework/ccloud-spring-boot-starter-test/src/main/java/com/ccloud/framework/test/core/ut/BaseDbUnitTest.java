package com.ccloud.framework.test.core.ut;

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.ccloud.framework.mybatis.config.CcloudMybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;

/**
 * 数据库单测基类。
 *
 * <p>跑在真实 MySQL 8.4（Testcontainers）上，表结构来自仓库的正式 DDL：
 * 唯一约束、bit(1)、JSON 列、ON DUPLICATE KEY 等 MySQL 语义都能如实验证，
 * 也不必额外维护一份会与生产漂移的 H2 建表脚本。容器每个 JVM 只启动一次。
 *
 * <p>只装配数据库相关的自动配置，不做组件扫描；被测 Service 由子类用 {@code @Import} 显式引入，
 * 其余依赖用 {@code @MockitoBean}。
 *
 * <p>每个测试方法运行在独立事务中并在结束后回滚。需要验证"业务事务回滚"的用例应在方法上声明
 * {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} 并自行清理数据。
 */
@SpringBootTest(classes = BaseDbUnitTest.Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("unit-test")
@Transactional
@ImportAutoConfiguration(classes = {
        DynamicDataSourceAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        CcloudMybatisAutoConfiguration.class
})
public abstract class BaseDbUnitTest {

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        MySQLContainer<?> mysql = MySqlTestContainer.get();
        String prefix = "spring.datasource.dynamic.datasource.master.";
        registry.add(prefix + "url", mysql::getJdbcUrl);
        registry.add(prefix + "username", mysql::getUsername);
        registry.add(prefix + "password", mysql::getPassword);
        registry.add(prefix + "driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Configuration(proxyBeanMethods = false)
    public static class Application {
    }
}
