package com.ccloud.module.infra.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * infra 业务配置。Nacos 配置刷新时会重新绑定
 */
@Component
@ConfigurationProperties(prefix = "ccloud.infra.api-log")
@Data
public class InfraProperties {

    /**
     * 访问日志的保留天数
     */
    private int accessRetainDays = 30;
    /**
     * 异常日志的保留天数
     */
    private int errorRetainDays = 90;
}
