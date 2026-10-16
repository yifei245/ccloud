package com.ccloud.module.pay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * pay 服务启动类。依赖 system 服务提供权限、租户等能力，本地运行前须先启动 system
 */
@SpringBootApplication
public class PayServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayServerApplication.class, args);
    }
}
