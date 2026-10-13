package com.ccloud.module.pay.framework.pay.core.client;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.CHANNEL_CONFIG_INVALID;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.CHANNEL_NOT_SUPPORTED;

/**
 * 渠道客户端工厂：按渠道编号缓存客户端；每次取用都带上库中的编码与配置，二者变化时重建。
 * 多实例部署时各实例在下一次取用时自行发现配置变化，无需广播
 */
@Component
public class PayClientFactory {

    public static final String MASK = "***";

    private final Map<String, PayClientProvider> providers;
    private final Map<Long, CachedClient> clients = new ConcurrentHashMap<>();

    public PayClientFactory(List<PayClientProvider> providerList) {
        Map<String, PayClientProvider> map = new HashMap<>();
        for (PayClientProvider provider : providerList) {
            if (map.putIfAbsent(provider.getCode(), provider) != null) {
                throw new IllegalStateException("渠道编码重复注册：" + provider.getCode());
            }
        }
        this.providers = Map.copyOf(map);
    }

    public boolean isSupported(String code) {
        return providers.containsKey(code);
    }

    public PayClientConfig parseConfig(String code, String configJson) {
        PayClientProvider provider = getProvider(code);
        PayClientConfig config = readJson(StrUtil.blankToDefault(configJson, "{}"), provider.getConfigClass());
        if (config == null) {
            throw exception(CHANNEL_CONFIG_INVALID, "不能为空");
        }
        try {
            config.validate();
        } catch (IllegalArgumentException e) {
            throw exception(CHANNEL_CONFIG_INVALID, e.getMessage());
        }
        return config;
    }

    public PayClient getPayClient(Long channelId, String code, String configJson) {
        return clients.compute(channelId, (id, cached) -> {
            if (cached != null && cached.code().equals(code) && cached.configJson().equals(configJson)) {
                return cached;
            }
            return new CachedClient(code, configJson, getProvider(code).create(id, parseConfig(code, configJson)));
        }).client();
    }

    public void invalidate(Long channelId) {
        clients.remove(channelId);
    }

    public String maskConfig(String code, String configJson) {
        Map<String, Object> map = toMap(configJson);
        for (String field : sensitiveFields(code)) {
            if (map.get(field) != null) {
                map.put(field, MASK);
            }
        }
        return JsonUtils.toJsonString(map);
    }

    public String mergeMaskedConfig(String code, String newJson, String oldJson) {
        Map<String, Object> merged = toMap(newJson);
        Map<String, Object> old = toMap(oldJson);
        for (String field : sensitiveFields(code)) {
            if (MASK.equals(merged.get(field))) {
                merged.put(field, old.get(field));
            }
        }
        return JsonUtils.toJsonString(merged);
    }

    private Set<String> sensitiveFields(String code) {
        return BeanUtils.instantiateClass(getProvider(code).getConfigClass()).sensitiveFields();
    }

    private static Map<String, Object> toMap(String json) {
        if (StrUtil.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> map;
        try {
            map = JsonUtils.getObjectMapper().readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw invalidJson();
        }
        return map == null ? new LinkedHashMap<>() : map;
    }

    /**
     * 解析渠道配置。不用 {@link JsonUtils#parseObject(String, Class)}：它解析失败时会把整段配置写入 ERROR 日志，
     * 而配置里含商户密钥。这里解析失败只抛异常，不记录配置内容
     */
    private static <T> T readJson(String json, Class<T> clazz) {
        try {
            return JsonUtils.getObjectMapper().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw invalidJson();
        }
    }

    private static ServiceException invalidJson() {
        return exception(CHANNEL_CONFIG_INVALID, "不是合法的 JSON");
    }

    private PayClientProvider getProvider(String code) {
        PayClientProvider provider = providers.get(code);
        if (provider == null) {
            throw exception(CHANNEL_NOT_SUPPORTED, code);
        }
        return provider;
    }

    private record CachedClient(String code, String configJson, PayClient client) {
    }

}
