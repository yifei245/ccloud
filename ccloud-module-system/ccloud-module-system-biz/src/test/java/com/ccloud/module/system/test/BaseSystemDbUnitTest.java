package com.ccloud.module.system.test;

import com.ccloud.framework.test.core.ut.LocalMySql;
import com.ccloud.framework.common.exception.ErrorCode;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.config.CcloudTenantAutoConfiguration;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.test.core.ut.BaseDbUnitTest;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.ccloud.framework.mybatis.config.CcloudMybatisAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * system 模块数据库单测基类：在框架基类之上加入多租户拦截器，默认以租户 1 执行。
 */
@ImportAutoConfiguration(classes = {
        DynamicDataSourceAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        CcloudMybatisAutoConfiguration.class,
        AopAutoConfiguration.class,
        CcloudTenantAutoConfiguration.class
})
@Import(BCryptPasswordEncoder.class)
// 需在配置数据加载前生效，否则 application.yaml 中的 optional:nacos 导入会尝试连接 Nacos
@TestPropertySource(properties = {
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.config.import-check.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false"
})
public abstract class BaseSystemDbUnitTest extends BaseDbUnitTest {

    protected static final Long TENANT_ID = 1L;

    static {
        LocalMySql.ensureInitialized();
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.beans.factory.ObjectProvider<org.springframework.cache.CacheManager> cacheManagerProvider;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
        cacheManagerProvider.ifAvailable(manager ->
                manager.getCacheNames().forEach(name -> manager.getCache(name).clear()));
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    protected static void assertServiceException(ThrowableAssert.ThrowingCallable callable, ErrorCode errorCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(errorCode.getCode());
    }
}
