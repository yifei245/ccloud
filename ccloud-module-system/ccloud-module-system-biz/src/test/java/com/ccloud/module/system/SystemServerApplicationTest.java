package com.ccloud.module.system;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ccloud.framework.i18n.core.DatabaseMessageSource;
import com.ccloud.framework.monitor.core.filter.TraceFilter;
import com.ccloud.framework.security.core.filter.LoginUserFilter;
import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.web.core.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * system 服务整体装配测试：真实 Redis（Testcontainers）+ H2，关闭 Nacos / XXL-Job / SBA / 链路上报。
 */
@SpringBootTest(classes = SystemServerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        // Nacos 开关必须在这里给：spring.config.import 解析早于 profile 配置文件生效
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false"
        })
@ActiveProfiles("unit-test")
@Testcontainers
class SystemServerApplicationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    private static final Pattern CREATE_TABLE =
            Pattern.compile("CREATE TABLE\\s+`?(\\w+)`?\\s*\\((.*?)\\)\\s*ENGINE", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private TenantProperties tenantProperties;

    @Test
    void 应用上下文应能正常启动且服务名与端口符合约定() {
        assertThat(ctx.getEnvironment().getProperty("spring.application.name")).isEqualTo("ccloud-system-server");
        // 测试用随机端口，这里校验的是 application.yaml 声明的默认端口
        assertThat(ctx.getEnvironment().getProperty("server.port")).isNotBlank();
    }

    @Test
    void 关键框架Bean应被装配() {
        assertThat(ctx.getBeansOfType(GlobalExceptionHandler.class)).hasSize(1);
        assertThat(ctx.getBeansOfType(LoginUserFilter.class)).hasSize(1);
        assertThat(filterRegistrations(TraceFilter.class)).singleElement()
                .satisfies(r -> assertThat(r.isEnabled()).isTrue());
        // LoginUserFilter 只应运行在 Security 过滤器链内，不能再被 Boot 注册进 Servlet 容器
        assertThat(filterRegistrations(LoginUserFilter.class)).singleElement()
                .satisfies(r -> assertThat(r.isEnabled()).isFalse());
        assertThat(ctx.getBean(PasswordEncoder.class)).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(ctx.getBean("messageSource", MessageSource.class)).isInstanceOf(DatabaseMessageSource.class);
        // 缓存切面按类型解析 CacheManager（LoadBalancer 另有一个内部实现），必须解析到按租户隔离的实现
        assertThat(ctx.getBean(org.springframework.cache.CacheManager.class))
                .isInstanceOf(com.ccloud.framework.tenant.core.redis.TenantRedisCacheManager.class);
        assertThat(ctx.getBeansOfType(com.ccloud.framework.security.core.service.PermissionCacheRefresher.class)).hasSize(1);
        // 本地消息表的投递以 Redis 为条件，自动配置顺序不对时会被静默跳过，业务发消息直接报找不到 Bean
        assertThat(ctx.getBeansOfType(com.ccloud.framework.mq.outbox.core.OutboxMessageService.class)).hasSize(1);
        // 短信等异步发送依赖 Stream 消费者，缺了它消息只进不出
        assertThat(ctx.getBean(com.ccloud.framework.mq.redis.core.RedisStreamMessageConsumer.class).isRunning()).isTrue();
        // @OperateLog 须写入数据库，而不是框架默认的只打日志
        assertThat(ctx.getBean(com.ccloud.framework.operatelog.core.service.OperateLogFrameworkService.class))
                .isInstanceOf(com.ccloud.module.system.service.logger.OperateLogFrameworkServiceImpl.class);
        assertThat(ctx.getBeansOfType(com.ccloud.framework.operatelog.core.aop.OperateLogAspect.class)).hasSize(1);
        // 数据库文案须被加载进 messageSource，并能收到其他节点的刷新通知
        assertThat(ctx.getBeansOfType(com.ccloud.framework.i18n.core.I18nMessageRefresher.class)).hasSize(1);
        // 多实例部署下站内信等实时推送必须经 Redis 广播，否则连接在其他节点上的用户收不到
        assertThat(ctx.getBean(com.ccloud.framework.websocket.core.sender.WebSocketMessageSender.class))
                .isInstanceOf(com.ccloud.framework.websocket.core.sender.RedisWebSocketMessageSender.class);
    }

    @Test
    void MP拦截器顺序应为租户_数据权限_分页() {
        List<InnerInterceptor> list = ctx.getBean(MybatisPlusInterceptor.class).getInterceptors();
        assertThat(list).hasSize(3);
        assertThat(list.get(0)).isInstanceOf(TenantLineInnerInterceptor.class);
        assertThat(list.get(1)).isInstanceOf(DataPermissionInterceptor.class);
        // mybatis-plus-join 会把分页拦截器包装成 PageInnerInterceptorWrapper（其子类）
        assertThat(list.get(2)).isInstanceOf(PaginationInnerInterceptor.class);
    }

    /**
     * 租户忽略表必须与 DDL 严格一致：
     * 少配 → 全局表被拼上 tenant_id 条件，SQL 直接报列不存在；
     * 多配 → 租户表失去隔离，数据跨租户泄露。
     */
    @Test
    void 租户忽略表应恰好是DDL中所有不含tenant_id列的表() throws IOException {
        Set<String> withoutTenantId = new TreeSet<>();
        Set<String> allTables = new TreeSet<>();
        Path ddlDir = locateDdlDir();
        try (Stream<Path> files = Files.list(ddlDir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".sql")).sorted().toList()) {
                Matcher m = CREATE_TABLE.matcher(Files.readString(file));
                while (m.find()) {
                    allTables.add(m.group(1));
                    if (!m.group(2).matches("(?s).*`tenant_id`.*")) {
                        withoutTenantId.add(m.group(1));
                    }
                }
            }
        }
        assertThat(allTables).as("DDL 解析结果应覆盖全部表").hasSize(143);
        assertThat(new TreeSet<>(tenantProperties.getIgnoreTables())).isEqualTo(withoutTenantId);
    }

    /**
     * 缓存全局表数据的缓存必须全局共享：否则系统租户修改后只清掉自己那份，其他租户持续读到旧数据
     */
    @Test
    void 全局数据的缓存应登记为不按租户隔离() {
        assertThat(tenantProperties.getIgnoreCaches()).contains(
                cacheName(com.ccloud.module.system.dal.redis.RedisKeyConstants.PERMISSION_MENU_ID_LIST),
                cacheName(com.ccloud.module.system.dal.redis.RedisKeyConstants.DICT_DATA_LIST),
                cacheName(com.ccloud.module.system.dal.redis.RedisKeyConstants.OAUTH2_CLIENT),
                cacheName(com.ccloud.module.system.dal.redis.RedisKeyConstants.I18N_MESSAGE_MAP));
    }

    private static String cacheName(String nameWithTtl) {
        int index = nameWithTtl.indexOf('#');
        return index == -1 ? nameWithTtl : nameWithTtl.substring(0, index);
    }

    private List<FilterRegistrationBean<?>> filterRegistrations(Class<?> filterClass) {
        List<FilterRegistrationBean<?>> result = new ArrayList<>();
        for (FilterRegistrationBean<?> r : ctx.getBeansOfType(FilterRegistrationBean.class).values()) {
            if (filterClass.isInstance(r.getFilter())) {
                result.add(r);
            }
        }
        return result;
    }

    private static Path locateDdlDir() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("sql/mysql/ddl");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("未找到 sql/mysql/ddl 目录");
    }
}
