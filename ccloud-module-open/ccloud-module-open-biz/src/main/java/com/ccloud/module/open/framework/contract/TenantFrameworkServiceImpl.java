package com.ccloud.module.open.framework.contract;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.core.service.TenantFrameworkService;
import com.ccloud.module.system.api.tenant.TenantApi;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 租户校验经 RPC 由 system 判断。每个请求都要校验，结果在本地缓存；
 * 租户被禁用或过期后，最长在缓存时长内仍可访问本服务
 */
@Component
public class TenantFrameworkServiceImpl implements TenantFrameworkService {

    private final TenantApi tenantApi;
    /**
     * 租户编号 → 校验失败的异常；空表示校验通过
     */
    private final Cache<Long, Optional<ServiceException>> validCache;

    public TenantFrameworkServiceImpl(@Lazy TenantApi tenantApi,
                                      @Value("${ccloud.tenant.valid-cache-duration:1m}") Duration cacheDuration) {
        this.tenantApi = tenantApi;
        this.validCache = Caffeine.newBuilder().expireAfterWrite(cacheDuration).maximumSize(10_000).build();
    }

    @Override
    public List<Long> getTenantIds() {
        return tenantApi.getTenantIdList().getCheckedData();
    }

    @Override
    public void validTenant(Long id) {
        // system 不可用等非业务异常不缓存，直接抛出，下次请求重试
        Optional<ServiceException> result = validCache.get(id, key -> {
            try {
                tenantApi.validTenant(key).getCheckedData();
                return Optional.empty();
            } catch (ServiceException e) {
                return Optional.of(e);
            }
        });
        if (result.isPresent()) {
            throw result.get();
        }
    }

    /**
     * 不缓存：只在少数管理操作中调用
     */
    @Override
    public boolean isSystemTenant(Long id) {
        return Boolean.TRUE.equals(tenantApi.isSystemTenant(id).getCheckedData());
    }
}
