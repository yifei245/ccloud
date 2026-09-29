package com.ccloud.framework.mybatis.config;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.context.properties.ConfigurationPropertiesReportEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.context.properties.ConfigurationPropertiesReportEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * 替换 actuator 的配置属性端点 {@code /actuator/configprops}，避免它耗尽数据库连接池。
 *
 * <p>该端点用 Jackson 逐个调用配置属性 bean 的 getter 生成报告。MyBatis-Plus 的 {@code mybatis-plus} 配置中，
 * {@code GlobalConfig} 持有 {@link SqlSessionFactory}，顺着它能走到每个 SQL 映射所在的 Configuration 与数据源，
 * 最终调用 {@code DataSource.getConnection()}——在 Jackson 看来这是一个名为 connection 的属性。
 * 每访问一次端点就借出大量连接且永不归还，监控面板打开几次"配置属性"页面，服务就会因连接池耗尽而不可用。
 *
 * <p>这里在序列化时忽略数据源与 {@link SqlSessionFactory} 类型的属性（它们是运行时对象，不是配置），其余照常展示
 */
@AutoConfiguration(before = ConfigurationPropertiesReportEndpointAutoConfiguration.class)
@ConditionalOnClass({ConfigurationPropertiesReportEndpoint.class, SqlSessionFactory.class})
@ConditionalOnAvailableEndpoint(endpoint = ConfigurationPropertiesReportEndpoint.class)
@EnableConfigurationProperties(ConfigurationPropertiesReportEndpointProperties.class)
public class CcloudConfigPropsEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint(
            ConfigurationPropertiesReportEndpointProperties properties,
            ObjectProvider<SanitizingFunction> sanitizingFunctions) {
        return new SafeConfigurationPropertiesReportEndpoint(sanitizingFunctions.orderedStream().toList(),
                properties.getShowValues());
    }

    static class SafeConfigurationPropertiesReportEndpoint extends ConfigurationPropertiesReportEndpoint {

        SafeConfigurationPropertiesReportEndpoint(Iterable<SanitizingFunction> sanitizingFunctions, Show showValues) {
            super(sanitizingFunctions, showValues);
        }

        @Override
        protected void configureJsonMapper(JsonMapper.Builder builder) {
            super.configureJsonMapper(builder);
            // 混入到接口上，对全部实现类生效（DynamicRoutingDataSource、DruidDataSource、DefaultSqlSessionFactory 等）
            builder.addMixIn(DataSource.class, IgnoredType.class);
            builder.addMixIn(SqlSessionFactory.class, IgnoredType.class);
        }
    }

    @JsonIgnoreType
    private abstract static class IgnoredType {
    }
}
