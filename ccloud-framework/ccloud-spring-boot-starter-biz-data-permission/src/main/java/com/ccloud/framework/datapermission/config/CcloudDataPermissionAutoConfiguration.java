package com.ccloud.framework.datapermission.config;

import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.ccloud.framework.datapermission.core.aop.DataPermissionAnnotationAdvisor;
import com.ccloud.framework.datapermission.core.db.DataPermissionRuleHandler;
import com.ccloud.framework.datapermission.core.rule.DataPermissionRule;
import com.ccloud.framework.datapermission.core.rule.DataPermissionRuleFactory;
import com.ccloud.framework.datapermission.core.rule.DataPermissionRuleFactoryImpl;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionFrameworkService;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * 数据权限自动配置
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "ccloud.data-permission", value = "enable", matchIfMissing = true)
public class CcloudDataPermissionAutoConfiguration {

    @Bean
    public DataPermissionRuleFactory dataPermissionRuleFactory(List<DataPermissionRule> rules) {
        return new DataPermissionRuleFactoryImpl(rules);
    }

    /**
     * {@code @Order(-50)} 使其排在租户拦截器（-100）之后、分页之前。
     * 租户是"能看到哪个租户的数据"，数据权限是"在本租户内能看到哪些行"，
     * 前者必须先生效。
     */
    @Bean
    @Order(-50)
    public DataPermissionInterceptor dataPermissionInterceptor(DataPermissionRuleFactory ruleFactory) {
        return new DataPermissionInterceptor(new DataPermissionRuleHandler(ruleFactory));
    }

    @Bean
    public DataPermissionAnnotationAdvisor dataPermissionAnnotationAdvisor(
            org.springframework.beans.factory.ObjectProvider<DataPermissionRuleFactory> ruleFactoryProvider) {
        return new DataPermissionAnnotationAdvisor(ruleFactoryProvider);
    }

    /**
     * 部门规则。仅当 system 模块提供了查询实现时才注册。
     */
    @Bean
    @ConditionalOnBean(DeptDataPermissionFrameworkService.class)
    public DeptDataPermissionRule deptDataPermissionRule(
            DeptDataPermissionFrameworkService deptDataPermissionFrameworkService,
            List<DeptDataPermissionRuleCustomizer> customizers) {
        DeptDataPermissionRule rule = new DeptDataPermissionRule(deptDataPermissionFrameworkService);
        customizers.forEach(customizer -> customizer.customize(rule));
        return rule;
    }
}
