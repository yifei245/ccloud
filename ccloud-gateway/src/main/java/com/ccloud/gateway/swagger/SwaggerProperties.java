package com.ccloud.gateway.swagger;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ccloud.swagger")
@Data
public class SwaggerProperties {

    /**
     * 是否开放接口文档聚合页。生产环境关闭：doc.html、分组配置与文档转发路由一并不注册
     */
    private boolean enable = true;
}
