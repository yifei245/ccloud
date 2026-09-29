package com.ccloud.framework.mybatis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 持久层配置
 */
@ConfigurationProperties(prefix = "ccloud.mybatis")
@Data
public class MybatisProperties {

    /**
     * 数据库字段加密密钥，长度必须是 16 / 24 / 32。
     * 使用了 {@code EncryptTypeHandler} 的模块必须配置，否则启动即失败——
     * 与其运行时才发现读不出数据，不如启动时就报出来。
     */
    private String encryptKey;
}
