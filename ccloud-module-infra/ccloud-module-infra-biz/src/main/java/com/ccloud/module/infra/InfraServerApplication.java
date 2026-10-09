package com.ccloud.module.infra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * infra 服务启动类。依赖 system 服务提供权限、租户等能力，本地运行前须先启动 system
 */
@SpringBootApplication
public class InfraServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfraServerApplication.class, args);
    }
}
