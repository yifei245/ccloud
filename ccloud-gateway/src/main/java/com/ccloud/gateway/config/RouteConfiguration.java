package com.ccloud.gateway.config;

import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.gateway.util.WebFrameworkUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * 路由规则。
 *
 * <p>不做 StripPrefix：下游 Controller 的映射本身就带 {@code /admin-api/{模块}} 前缀，
 * 网关与直连服务使用同一套路径，排查问题时不必换算。
 *
 * <p>{@code /open-api/{模块}} 只路由到 {@link #MODULES} 中的模块。按路径段拼服务名会让任何人
 * 借一个路径访问注册中心里的任意服务（包括网关自身、监控服务的 actuator）。
 */
@Configuration(proxyBeanMethods = false)
public class RouteConfiguration {

    /**
     * 业务模块，对应服务名 {@code ccloud-{模块}-server}。新增模块在此登记
     */
    static final List<String> MODULES = List.of("system", "infra", "member", "open",
            "pay", "bpm", "product", "trade", "promotion", "ai");

    static final String RPC_API_PATTERN = "/rpc-api/**";

    @Bean
    public RouteLocator ccloudRouteLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        // 内部接口只供服务间直连调用，经网关一律拒绝。最先匹配，不给任何业务路由机会
        routes.route("rpc-block", r -> r.order(Ordered.HIGHEST_PRECEDENCE)
                .path(RPC_API_PATTERN)
                .filters(f -> f.filter(forbidden()))
                .uri("no://op"));
        routes.route("system-sso", r -> r.path("/sso/**", "/oauth2/**")
                .uri(serviceUri("system")));
        for (String module : MODULES) {
            routes.route(module, r -> r.path("/admin-api/" + module + "/**", "/app-api/" + module + "/**")
                    .uri(serviceUri(module)));
            routes.route("open-api-" + module, r -> r.path("/open-api/" + module + "/**")
                    .uri(serviceUri(module)));
        }
        return routes.build();
    }

    static String serviceUri(String module) {
        return "lb://ccloud-" + module + "-server";
    }

    private static GatewayFilter forbidden() {
        return (exchange, chain) -> WebFrameworkUtils.writeJSON(exchange, HttpStatus.FORBIDDEN,
                CommonResult.error(GlobalErrorCodeConstants.FORBIDDEN.getCode(), "禁止从外部访问内部接口"));
    }
}
