package com.ccloud.gateway.service.auth;

import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.gateway.filter.security.LoginUser;
import com.ccloud.gateway.filter.security.SecurityProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 经 WebClient 直调 system 的令牌校验 RPC（不用 Feign：Feign 是阻塞式调用，会占住 WebFlux 的事件循环线程）。
 *
 * <p>结果按令牌缓存，无效令牌也缓存，避免随机令牌打穿到 system。调用失败不缓存（Caffeine 会移除异常结束的 future）。
 */
@Slf4j
@Service
public class TokenCheckServiceImpl implements TokenCheckService {

    static final String CHECK_URL = "http://ccloud-system-server/rpc-api/system/oauth2/token/check?accessToken={token}";

    private final WebClient webClient;
    private final SecurityProperties securityProperties;
    private final AsyncCache<String, Optional<LoginUser>> cache;

    public TokenCheckServiceImpl(WebClient.Builder loadBalancedWebClientBuilder, SecurityProperties securityProperties) {
        this.webClient = loadBalancedWebClientBuilder.build();
        this.securityProperties = securityProperties;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(securityProperties.getTokenCacheTtl())
                .maximumSize(securityProperties.getTokenCacheMaxSize())
                .buildAsync();
    }

    @Override
    public Mono<LoginUser> checkAccessToken(String accessToken) {
        return get(accessToken).flatMap(cached -> {
            // 缓存期内令牌到期：不能继续放行，重新向 system 确认
            if (cached.isPresent() && cached.get().isExpired()) {
                invalidate(accessToken);
                return get(accessToken);
            }
            return Mono.just(cached);
        }).flatMap(Mono::justOrEmpty);
    }

    private Mono<Optional<LoginUser>> get(String accessToken) {
        return Mono.defer(() -> {
            CompletableFuture<Optional<LoginUser>> future =
                    cache.get(accessToken, (token, executor) -> remoteCheck(token).toFuture());
            // suppressCancel：多个请求共享同一个 future，一个请求取消不能连带取消其他请求的校验。
            // 失败时立即移除：Caffeine 对异常结束的 future 是异步移除的，system 恢复后紧接着的请求仍可能拿到旧的失败结果
            return Mono.fromFuture(future, true).doOnError(e -> cache.asMap().remove(accessToken, future));
        });
    }

    private Mono<Optional<LoginUser>> remoteCheck(String accessToken) {
        return webClient.get().uri(CHECK_URL, accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(securityProperties.getTokenCheckTimeout())
                .onErrorMap(e -> new TokenCheckUnavailableException("令牌校验服务不可用", e))
                .map(TokenCheckServiceImpl::parse);
    }

    private static Optional<LoginUser> parse(String body) {
        CommonResult<CheckRespDTO> result;
        try {
            result = JsonUtils.parseObject(body, new TypeReference<>() {});
        } catch (RuntimeException e) {
            throw new TokenCheckUnavailableException("令牌校验响应无法解析", e);
        }
        if (result == null) {
            throw new TokenCheckUnavailableException("令牌校验响应为空", null);
        }
        if (Objects.equals(result.getCode(), GlobalErrorCodeConstants.UNAUTHORIZED.getCode())) {
            return Optional.empty();
        }
        // 其他错误码（system 内部异常、租户校验失败等）不代表令牌无效，按不可用处理
        if (!result.isSuccess() || result.getData() == null) {
            throw new TokenCheckUnavailableException("令牌校验返回错误码 " + result.getCode(), null);
        }
        return Optional.of(result.getData().toLoginUser());
    }

    @Override
    public void invalidate(String accessToken) {
        cache.synchronous().invalidate(accessToken);
    }

    /**
     * system OAuth2AccessTokenCheckRespDTO 的对应结构
     */
    @Data
    static class CheckRespDTO {

        private Long userId;
        private Integer userType;
        private Map<String, String> userInfo;
        private Long tenantId;
        private String clientId;
        private List<String> scopes;
        private LocalDateTime expiresTime;

        LoginUser toLoginUser() {
            LoginUser loginUser = new LoginUser();
            loginUser.setId(userId);
            loginUser.setUserType(userType);
            loginUser.setInfo(userInfo);
            loginUser.setTenantId(tenantId);
            loginUser.setClientId(clientId);
            loginUser.setScopes(scopes);
            loginUser.setExpiresTime(expiresTime);
            return loginUser;
        }
    }
}
