package com.ccloud.gateway.filter.security;

import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.gateway.service.auth.TokenCheckService;
import com.ccloud.gateway.service.auth.TokenCheckUnavailableException;
import com.ccloud.gateway.util.SecurityFrameworkUtils;
import com.ccloud.gateway.util.WebFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 令牌校验与内部身份头下发。
 *
 * <ul>
 *   <li>令牌有效：写入内部身份头后转发，白名单接口也写（匿名可调不等于忽略身份）；</li>
 *   <li>无令牌或令牌无效：白名单接口按匿名转发，其余返回 401；</li>
 *   <li>system 不可用：白名单接口按匿名转发，其余返回 503——不能误判为 401 把用户踢下线。</li>
 * </ul>
 * 开放接口（{@code /open-api/**}）由 {@code OpenApiSignatureFilter} 按签名认证，此处跳过
 */
@Slf4j
@Component
public class TokenAuthenticationFilter implements GlobalFilter, Ordered {

    public static final int ORDER = -100;

    private static final String OPEN_API_PREFIX = "/open-api/";

    private final TokenCheckService tokenCheckService;
    private final SecurityProperties securityProperties;
    private final IgnoreUrlsMatcher ignoreUrlsMatcher;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TokenAuthenticationFilter(TokenCheckService tokenCheckService, SecurityProperties securityProperties) {
        this.tokenCheckService = tokenCheckService;
        this.securityProperties = securityProperties;
        this.ignoreUrlsMatcher = new IgnoreUrlsMatcher(securityProperties.getIgnoreUrls());
    }

    /**
     * 校验结果：user 非空为已登录；unavailable 为 system 不可用
     */
    private record Outcome(LoginUser user, boolean unavailable) {
        static final Outcome ANONYMOUS = new Outcome(null, false);
        static final Outcome UNAVAILABLE = new Outcome(null, true);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (path.startsWith(OPEN_API_PREFIX)) {
            return chain.filter(exchange);
        }
        boolean anonymousAllowed = ignoreUrlsMatcher.matches(exchange.getRequest().getMethod(), path);
        String token = SecurityFrameworkUtils.obtainAccessToken(exchange.getRequest());
        if (token == null) {
            return anonymousAllowed ? chain.filter(exchange) : unauthorized(exchange);
        }

        return tokenCheckService.checkAccessToken(token)
                .map(user -> new Outcome(user, false))
                .defaultIfEmpty(Outcome.ANONYMOUS)
                .onErrorResume(TokenCheckUnavailableException.class, e -> {
                    log.error("[filter] 令牌校验失败 path={}", path, e);
                    return Mono.just(Outcome.UNAVAILABLE);
                })
                .flatMap(outcome -> {
                    if (outcome.user() != null) {
                        ServerWebExchange authenticated = exchange.mutate().request(InternalHeaderWriter.write(
                                exchange.getRequest(), outcome.user(), securityProperties.getInternalSignSecret())).build();
                        return afterLogout(chain.filter(authenticated), path, token);
                    }
                    if (anonymousAllowed) {
                        return chain.filter(exchange);
                    }
                    return outcome.unavailable() ? unavailable(exchange) : unauthorized(exchange);
                });
    }

    /**
     * 登出请求结束后清除本地缓存，否则被作废的令牌在缓存期内仍能通过网关
     */
    private Mono<Void> afterLogout(Mono<Void> filtered, String path, String token) {
        boolean logout = securityProperties.getLogoutUrls().stream().anyMatch(p -> pathMatcher.match(p, path));
        return logout ? filtered.doFinally(signal -> tokenCheckService.invalidate(token)) : filtered;
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange) {
        return WebFrameworkUtils.writeJSON(exchange, HttpStatus.UNAUTHORIZED,
                CommonResult.error(GlobalErrorCodeConstants.UNAUTHORIZED));
    }

    private static Mono<Void> unavailable(ServerWebExchange exchange) {
        return WebFrameworkUtils.writeJSON(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                CommonResult.error(HttpStatus.SERVICE_UNAVAILABLE.value(), "认证服务暂不可用，请稍后重试"));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
