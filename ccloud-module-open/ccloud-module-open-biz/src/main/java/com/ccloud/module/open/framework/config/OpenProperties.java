package com.ccloud.module.open.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "ccloud.open")
@Component
@Validated
@Data
public class OpenProperties {

    private Notify notify = new Notify();
    private ApiLog apiLog = new ApiLog();

    @Data
    public static class Notify {
        /**
         * 是否允许回调到回环、内网等地址。默认 false 防止 SSRF；仅本地开发与 docker 联调打开。
         * 打开后云元数据地址（169.254.169.254、fd00:ec2::254）仍然拒绝
         */
        private boolean allowPrivateAddress = false;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(5);
    }

    @Data
    public static class ApiLog {
        /**
         * 调用日志与已结束（成功、放弃）的回调日志的保留天数
         */
        private int retainDays = 30;
    }
}
