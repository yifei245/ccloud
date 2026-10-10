package com.ccloud.gateway.service.open;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.gateway.filter.openapi.OpenApiProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 经 WebClient 直调 open 的应用快照 RPC（不用 Feign：Feign 是阻塞式调用，会占住 WebFlux 的事件循环线程）。
 *
 * <p>结果按 appKey 缓存：存在的应用缓存较久，open 变更后会广播失效；不存在的应用短暂缓存，
 * 防止随机 appKey 打穿到 open，又能让新建的应用尽快可用。调用失败不缓存
 */
@Slf4j
@Service
public class OpenAppServiceImpl implements OpenAppService {

    static final String GET_URL = "http://ccloud-open-server/rpc-api/open/app/get-by-key?appKey={appKey}";

    private final WebClient webClient;
    private final OpenApiProperties properties;
    private final AsyncCache<String, Optional<OpenApp>> cache;

    public OpenAppServiceImpl(WebClient.Builder loadBalancedWebClientBuilder, OpenApiProperties properties) {
        this.webClient = loadBalancedWebClientBuilder.build();
        this.properties = properties;
        this.cache = Caffeine.newBuilder()
                .expireAfter(Expiry.<String, Optional<OpenApp>>creating((key, value) ->
                        value.isPresent() ? properties.getAppCacheTtl() : properties.getAppCacheNotFoundTtl()))
                .maximumSize(properties.getAppCacheMaxSize())
                .buildAsync();
    }

    @Override
    public Mono<OpenApp> getApp(String appKey) {
        return Mono.defer(() -> {
            CompletableFuture<Optional<OpenApp>> future =
                    cache.get(appKey, (key, executor) -> remoteGet(key).toFuture());
            // suppressCancel：多个请求共享同一个 future，一个请求取消不能连带取消其他请求的查询。
            // 失败时立即移除：Caffeine 对异常结束的 future 是异步移除的，open 恢复后紧接着的请求仍可能拿到旧的失败结果
            return Mono.fromFuture(future, true).doOnError(e -> cache.asMap().remove(appKey, future));
        }).flatMap(Mono::justOrEmpty);
    }

    private Mono<Optional<OpenApp>> remoteGet(String appKey) {
        return webClient.get().uri(GET_URL, appKey)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(properties.getAppLookupTimeout())
                .onErrorMap(e -> new OpenAppUnavailableException("开放平台服务不可用", e))
                .map(OpenAppServiceImpl::parse);
    }

    private static Optional<OpenApp> parse(String body) {
        CommonResult<OpenApp> result;
        try {
            result = JsonUtils.parseObject(body, new TypeReference<>() {});
        } catch (RuntimeException e) {
            throw new OpenAppUnavailableException("应用查询响应无法解析", e);
        }
        if (result == null || !result.isSuccess()) {
            throw new OpenAppUnavailableException(
                    "应用查询返回错误码 " + (result == null ? null : result.getCode()), null);
        }
        return Optional.ofNullable(result.getData());
    }

    @Override
    public void invalidate(String appKey) {
        cache.synchronous().invalidate(appKey);
    }

    @Override
    public void invalidateAll() {
        cache.synchronous().invalidateAll();
    }
}
