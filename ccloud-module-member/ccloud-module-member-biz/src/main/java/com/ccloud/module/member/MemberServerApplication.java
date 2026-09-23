package com.ccloud.module.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * member 服务启动类。依赖 system 服务提供权限、租户、令牌等能力，本地运行前须先启动 system
 */
@SpringBootApplication
public class MemberServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberServerApplication.class, args);
    }
}
