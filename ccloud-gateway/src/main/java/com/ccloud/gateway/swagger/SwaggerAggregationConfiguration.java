package com.ccloud.gateway.swagger;

import com.ccloud.gateway.config.RouteConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * 接口文档聚合：doc.html 页面（Knife4j 静态资源）、分组配置、以及把 {@code /{模块}/v3/api-docs/{分组}}
 * 转发到对应服务 {@code /v3/api-docs/{分组}} 的路由。{@code ccloud.swagger.enable=false} 时整体不注册。
 *
 * <p>不使用 knife4j-gateway-spring-boot-starter：它按"服务名作路径前缀并去掉前缀"的约定生成文档地址，
 * 与网关按模块、不去前缀的路由规则不符，也无法表达每个服务的三个分组
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SwaggerProperties.class)
@ConditionalOnProperty(prefix = "ccloud.swagger", name = "enable", havingValue = "true", matchIfMissing = true)
public class SwaggerAggregationConfiguration {

    private static final String UI_LOCATION = "META-INF/resources/";

    @Bean
    public RouterFunction<ServerResponse> swaggerRouterFunction(ReactiveDiscoveryClient discoveryClient) {
        SwaggerResourceHandler handler = new SwaggerResourceHandler(discoveryClient, RouteConfiguration.MODULES);
        ClassPathResource docHtml = new ClassPathResource(UI_LOCATION + "doc.html");
        return RouterFunctions.route(GET("/v3/api-docs/swagger-config"), handler::swaggerConfig)
                .andRoute(GET("/doc.html"), request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(docHtml))
                .and(RouterFunctions.resources("/webjars/**", new ClassPathResource(UI_LOCATION + "webjars/")))
                .and(RouterFunctions.resources("/img/**", new ClassPathResource(UI_LOCATION + "img/")));
    }

    /**
     * 只把 admin / app / open 三个分组改写到下游：改写规则一旦写宽（如 {@code /{模块}/**} 去前缀），
     * 就能借文档路由访问下游的 {@code /rpc-api}、{@code /actuator}
     */
    @Bean
    public RouteLocator swaggerRouteLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        for (String module : RouteConfiguration.MODULES) {
            routes.route("swagger-" + module, r -> r
                    .path("/" + module + "/v3/api-docs/admin", "/" + module + "/v3/api-docs/app",
                            "/" + module + "/v3/api-docs/open")
                    .filters(f -> f.rewritePath("^/" + module + "/v3/api-docs/(?<group>admin|app|open)$",
                            "/v3/api-docs/${group}"))
                    .uri(RouteConfiguration.serviceUri(module)));
        }
        return routes.build();
    }
}
