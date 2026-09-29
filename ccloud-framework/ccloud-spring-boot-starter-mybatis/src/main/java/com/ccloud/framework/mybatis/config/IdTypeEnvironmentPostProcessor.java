package com.ccloud.framework.mybatis.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 依据数据源 URL 自动推断主键策略。
 *
 * <p>省掉一处必填配置：MySQL 用自增主键即可，不必在每个服务的 yaml 里重复声明。
 * 已显式配置时不覆盖。
 */
public class IdTypeEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String ID_TYPE_KEY = "mybatis-plus.global-config.db-config.id-type";
    private static final String DATASOURCE_URL_KEY = "spring.datasource.url";
    private static final String DATASOURCE_DYNAMIC_URL_KEY = "spring.datasource.dynamic.datasource.master.url";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (StrUtil.isNotBlank(environment.getProperty(ID_TYPE_KEY))) {
            return;
        }
        String url = environment.getProperty(DATASOURCE_DYNAMIC_URL_KEY);
        if (StrUtil.isBlank(url)) {
            url = environment.getProperty(DATASOURCE_URL_KEY);
        }
        if (StrUtil.isBlank(url)) {
            return;
        }
        if (url.toLowerCase().contains(":mysql:")) {
            environment.getSystemProperties().put(ID_TYPE_KEY, IdType.AUTO.name());
        }
    }
}
