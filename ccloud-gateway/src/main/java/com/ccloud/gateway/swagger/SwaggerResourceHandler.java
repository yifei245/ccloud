package com.ccloud.gateway.swagger;

import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 生成 Knife4j 读取的 {@code /v3/api-docs/swagger-config}：从注册中心发现在线的业务服务，
 * 每个服务列出 admin / app / open 三个分组，文档地址指向网关的文档转发路由 {@code /{模块}/v3/api-docs/{分组}}
 */
public class SwaggerResourceHandler {

    private static final Pattern SERVICE_NAME = Pattern.compile("^ccloud-(.+)-server$");

    /**
     * 分组编号与展示名。不含 rpc 分组：内部接口不经网关展示
     */
    static final Map<String, String> GROUPS = new LinkedHashMap<>();

    static {
        GROUPS.put("admin", "管理端");
        GROUPS.put("app", "用户端");
        GROUPS.put("open", "开放接口");
    }

    private final ReactiveDiscoveryClient discoveryClient;
    private final List<String> modules;

    public SwaggerResourceHandler(ReactiveDiscoveryClient discoveryClient, List<String> modules) {
        this.discoveryClient = discoveryClient;
        this.modules = modules;
    }

    public Mono<ServerResponse> swaggerConfig(ServerRequest request) {
        return discoveryClient.getServices()
                .map(SERVICE_NAME::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                // 只列出网关有路由的模块，与 RouteConfiguration.MODULES 一致
                .filter(modules::contains)
                .distinct()
                .sort()
                .collectList()
                .flatMap(found -> {
                    List<Map<String, String>> urls = new ArrayList<>();
                    for (String module : found) {
                        GROUPS.forEach((group, label) -> urls.add(Map.of(
                                "name", module + "-" + label,
                                "url", "/" + module + "/v3/api-docs/" + group)));
                    }
                    Map<String, Object> config = new LinkedHashMap<>();
                    config.put("configUrl", "/v3/api-docs/swagger-config");
                    config.put("urls", urls);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(config);
                });
    }
}
