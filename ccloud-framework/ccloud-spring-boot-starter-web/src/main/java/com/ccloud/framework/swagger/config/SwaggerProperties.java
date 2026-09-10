package com.ccloud.framework.swagger.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Swagger 文档配置
 */
@ConfigurationProperties(prefix = "ccloud.swagger")
@Data
@Validated
public class SwaggerProperties {

    @NotEmpty(message = "标题不能为空")
    private String title = "ccloud 接口文档";

    @NotEmpty(message = "描述不能为空")
    private String description = "ccloud 微服务平台";

    @NotEmpty(message = "作者不能为空")
    private String author = "ccloud";

    @NotEmpty(message = "版本不能为空")
    private String version = "1.0.0";

    private String url = "https://github.com/yifei/ccloud";

    private String email = "";

    private String license = "MIT License";

    private String licenseUrl = "https://opensource.org/licenses/MIT";

    /**
     * 是否暴露 /rpc-api 分组。内部接口不该出现在对外文档里，
     * 生产环境必须保持 false。
     */
    private boolean exposeRpc = false;
}
