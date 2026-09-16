package com.ccloud.module.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * system 服务启动类
 *
 * <p>本地运行前需先起中间件：{@code docker compose -f docker/docker-compose.middleware.yml up -d}，
 * 并导入 sql/mysql/ccloud.sql。
 */
@SpringBootApplication
public class SystemServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemServerApplication.class, args);
    }
}
