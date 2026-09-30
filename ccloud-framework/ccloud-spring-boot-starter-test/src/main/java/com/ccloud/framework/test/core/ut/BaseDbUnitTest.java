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

/**
 * 数据库单测基类。
 *
 * <p>跑在本机已有的真实 MySQL 上（见 {@link LocalMySql}），表结构来自仓库的正式 DDL：
 * 唯一约束、bit(1)、JSON 列、ON DUPLICATE KEY 等 MySQL 语义都能如实验证，
 * 也不必额外维护一份会与生产漂移的 H2 建表脚本。表结构每个 JVM 只初始化一次。
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
        LocalMySql.register(registry);
    }

    @Configuration(proxyBeanMethods = false)
    public static class Application {
    }
}
