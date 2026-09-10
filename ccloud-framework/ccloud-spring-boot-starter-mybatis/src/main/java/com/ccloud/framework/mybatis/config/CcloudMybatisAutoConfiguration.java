package com.ccloud.framework.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.ccloud.framework.mybatis.core.handler.DefaultDBFieldHandler;
import com.ccloud.framework.mybatis.core.type.EncryptTypeHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * MyBatis Plus 自动配置。
 *
 * <p>拦截器顺序至关重要，必须是<b>租户 → 数据权限 → 分页</b>：
 * 分页拦截器会把原始 SQL 包进 {@code SELECT COUNT(*) FROM (...)} 或加上 LIMIT，
 * 一旦它先执行，后面的租户条件就拼不进内层查询，会造成跨租户数据泄漏。
 *
 * <p>各 starter 通过注册 {@link InnerInterceptor} Bean 并标注 {@code @Order} 来贡献拦截器，
 * 此处统一按 order 排序后装配，分页永远排在最后。
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MybatisProperties.class)
@MapperScan(value = "${ccloud.info.base-package:com.ccloud.module}",
        annotationClass = org.apache.ibatis.annotations.Mapper.class,
        lazyInitialization = "${mybatis.lazy-initialization:false}")
@RequiredArgsConstructor
public class CcloudMybatisAutoConfiguration {

    private final MybatisProperties mybatisProperties;

    @PostConstruct
    public void init() {
        EncryptTypeHandler.setEncryptKey(mybatisProperties.getEncryptKey());
    }

    /**
     * @param innerInterceptors 由租户、数据权限等 starter 贡献；不存在时为空列表
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(List<InnerInterceptor> innerInterceptors) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        List<InnerInterceptor> ordered = new ArrayList<>(innerInterceptors);
        ordered.sort(AnnotationAwareOrderComparator.INSTANCE);
        ordered.forEach(interceptor::addInnerInterceptor);

        // 分页永远最后：它会重写整条 SQL，必须让其他拦截器先把条件拼完
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        log.info("[mybatisPlusInterceptor] 拦截器装配顺序: {}",
                interceptor.getInterceptors().stream().map(i -> i.getClass().getSimpleName()).toList());
        return interceptor;
    }

    /**
     * 审计字段填充。登录用户由 security starter 覆盖该 Bean 提供，
     * 未引入时退化为不填充创建者/更新者。
     */
    @Bean
    @ConditionalOnMissingBean(DefaultDBFieldHandler.class)
    public DefaultDBFieldHandler defaultDBFieldHandler(
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            Supplier<Long> loginUserIdSupplier) {
        return new DefaultDBFieldHandler(loginUserIdSupplier);
    }
}
