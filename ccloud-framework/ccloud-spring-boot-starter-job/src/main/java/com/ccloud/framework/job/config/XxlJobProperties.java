package com.ccloud.framework.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * XXL-Job 配置
 */
@ConfigurationProperties(prefix = "xxl.job")
@Data
public class XxlJobProperties {

    private boolean enabled = true;

    private String accessToken;

    private Admin admin = new Admin();

    private Executor executor = new Executor();

    @Data
    public static class Admin {
        /**
         * 调度中心地址，多个用逗号分隔
         */
        private String addresses;
    }

    @Data
    public static class Executor {
        private String appname;
        private String address;
        private String ip;
        /**
         * 0 表示自动分配端口。多实例部署在同一台机器时必须用它，
         * 否则第二个实例会因端口冲突起不来
         */
        private Integer port = 0;
        private String logPath = "/data/applogs/xxl-job/jobhandler";
        private Integer logRetentionDays = 30;
    }
}
