package com.ccloud.module.pay;

import com.ccloud.framework.test.core.ut.LocalRedis;
import com.ccloud.framework.test.core.ut.LocalMySql;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ccloud.framework.apilog.core.service.ApiAccessLogFrameworkService;
import com.ccloud.framework.apilog.core.service.ApiErrorLogFrameworkService;
import com.ccloud.framework.excel.core.service.DictFrameworkService;
import com.ccloud.framework.i18n.core.I18nMessageFrameworkService;
import com.ccloud.framework.i18n.core.I18nMessageRefresher;
import com.ccloud.framework.operatelog.core.service.OperateLogFrameworkService;
import com.ccloud.framework.security.core.service.DenyAllSecurityFrameworkServiceImpl;
import com.ccloud.framework.security.core.service.PermissionFrameworkService;
import com.ccloud.framework.security.core.service.SecurityFrameworkService;
import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.tenant.core.service.TenantFrameworkService;
import com.ccloud.framework.tenant.core.web.TenantSecurityWebFilter;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.contract.*;
import com.ccloud.module.system.api.permission.PermissionApi;
import com.ccloud.module.system.api.tenant.TenantApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * pay 服务整体装配：真实 MySQL 与 Redis，关闭 Nacos / XXL-Job / SBA。
 *
 * <p>重点是框架契约：pay 没有权限、租户等数据，必须经 RPC 由 system 提供。
 * 缺一个实现，对应的安全机制就会静默失效（system 曾因此出现权限注解失效与跨租户访问）
 */
@SpringBootTest(classes = PayServerApplication.class,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false"
        })
@ActiveProfiles("unit-test")
class PayServerApplicationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        LocalMySql.register(registry);
        LocalRedis.register(registry);
    }

    private static final Pattern CREATE_TABLE =
            Pattern.compile("CREATE TABLE\\s+`?(\\w+)`?\\s*\\((.*?)\\)\\s*ENGINE", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private TenantProperties tenantProperties;

    @Test
    void 应用上下文应能正常启动且服务名符合约定() {
        assertThat(ctx.getEnvironment().getProperty("spring.application.name")).isEqualTo("ccloud-pay-server");
    }

    @Test
    void 单测环境开启了模拟支付_生产配置默认关闭() throws IOException {
        assertThat(ctx.getBean(PayProperties.class).isMockEnabled()).isTrue();
        String base = new String(new ClassPathResource("application.yaml").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String prod = new String(new ClassPathResource("application-prod.yaml").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(base).contains("mock-enabled: false");
        assertThat(prod).doesNotContain("mock-enabled: true");
    }

    @Test
    void 框架契约均由本服务经RPC实现() {
        assertThat(ctx.getBean(PermissionFrameworkService.class)).isInstanceOf(PermissionFrameworkServiceImpl.class);
        assertThat(ctx.getBean(TenantFrameworkService.class)).isInstanceOf(TenantFrameworkServiceImpl.class);
        assertThat(ctx.getBean(DictFrameworkService.class)).isInstanceOf(DictFrameworkServiceImpl.class);
        assertThat(ctx.getBean(I18nMessageFrameworkService.class)).isInstanceOf(I18nMessageFrameworkServiceImpl.class);
        assertThat(ctx.getBean(OperateLogFrameworkService.class)).isInstanceOf(OperateLogFrameworkServiceImpl.class);
        assertThat(ctx.getBean(ApiAccessLogFrameworkService.class)).isInstanceOf(ApiLogFrameworkServiceImpl.class);
        assertThat(ctx.getBean(ApiErrorLogFrameworkService.class)).isInstanceOf(ApiLogFrameworkServiceImpl.class);
    }

    @Test
    void 安全机制真正生效_权限表达式与租户校验() {
        assertThat(ctx.getBean("ss", SecurityFrameworkService.class))
                .as("不能是一律拒绝的兜底实现").isNotInstanceOf(DenyAllSecurityFrameworkServiceImpl.class);
        assertThat(ctx.getBeansOfType(FilterRegistrationBean.class).values())
                .as("租户安全过滤器：请求租户须与登录租户一致").anyMatch(r -> r.getFilter() instanceof TenantSecurityWebFilter);
        assertThat(ctx.getBeansOfType(I18nMessageRefresher.class)).as("数据库文案加载").hasSize(1);
    }

    @Test
    void 调用system的Feign客户端已注册() {
        assertThat(ctx.getBeanNamesForType(PermissionApi.class)).isNotEmpty();
        assertThat(ctx.getBeanNamesForType(TenantApi.class)).isNotEmpty();
    }

    @Test
    void MP拦截器顺序应为租户_分页() {
        List<InnerInterceptor> list = ctx.getBean(MybatisPlusInterceptor.class).getInterceptors();
        assertThat(list).hasSize(2);
        assertThat(list.get(0)).isInstanceOf(TenantLineInnerInterceptor.class);
        assertThat(list.get(1)).isInstanceOf(PaginationInnerInterceptor.class);
    }

    /**
     * 与 system 共用一个库：忽略表须与 DDL 严格一致，少配报错、多配泄露
     */
    @Test
    void 租户忽略表应恰好是DDL中所有不含tenant_id列的表() throws IOException {
        Set<String> withoutTenantId = new TreeSet<>();
        try (Stream<Path> files = Files.list(LocalMySql.locateDdlDir())) {
            for (Path file : files.filter(f -> f.toString().endsWith(".sql")).sorted().toList()) {
                Matcher m = CREATE_TABLE.matcher(Files.readString(file));
                while (m.find()) {
                    if (!m.group(2).matches("(?s).*`tenant_id`.*")) {
                        withoutTenantId.add(m.group(1));
                    }
                }
            }
        }
        assertThat(new TreeSet<>(tenantProperties.getIgnoreTables())).isEqualTo(withoutTenantId);
    }
}
