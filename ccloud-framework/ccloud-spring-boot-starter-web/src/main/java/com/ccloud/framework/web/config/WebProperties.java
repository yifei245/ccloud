package com.ccloud.framework.web.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Web 层配置
 */
@ConfigurationProperties(prefix = "ccloud.web")
@Data
@Validated
public class WebProperties {

    @NotNull(message = "管理端 API 配置不能为空")
    @Valid
    private Api adminApi = new Api("/admin-api");

    @NotNull(message = "用户端 API 配置不能为空")
    @Valid
    private Api appApi = new Api("/app-api");

    @NotNull(message = "开放 API 配置不能为空")
    @Valid
    private Api openApi = new Api("/open-api");

    @Data
    public static class Api {

        /**
         * 接口路径前缀
         */
        @NotNull(message = "API 前缀不能为空")
        private String prefix;

        public Api() {
        }

        public Api(String prefix) {
            this.prefix = prefix;
        }
    }
}
