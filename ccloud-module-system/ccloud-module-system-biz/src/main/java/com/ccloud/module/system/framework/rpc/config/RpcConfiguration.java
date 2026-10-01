package com.ccloud.module.system.framework.rpc.config;

import com.ccloud.module.infra.api.config.ConfigApi;
import com.ccloud.module.infra.api.file.FileApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * system 模块的 Feign 客户端。
 *
 * <p>刻意<b>不</b>把 system-api 自身的接口注册为 Feign 客户端：本服务内这些接口由
 * 本地 RestController 实现（Task 34），若同时注册 Feign 代理，按类型注入时会出现
 * 两个候选 Bean，且可能把本地调用绕成一次网络请求打回自己。
 * 本服务需要调用其他模块时，在 clients 中逐个显式列出。
 * 调用 infra：参数配置（初始密码、注册开关）、文件（头像）。
 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {ConfigApi.class, FileApi.class})
public class RpcConfiguration {
}
