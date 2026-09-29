package com.ccloud.framework.tenant.core.job;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.tenant.core.service.TenantFrameworkService;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link TenantJob} 的切面实现：遍历全部开启的租户逐一执行，并汇总结果。
 *
 * <p>单个租户失败不影响其他租户——一个租户的数据异常不该让全平台的任务停摆。
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class TenantJobAspect {

    private final TenantFrameworkService tenantFrameworkService;

    @Around("@annotation(tenantJob)")
    public String around(ProceedingJoinPoint joinPoint, TenantJob tenantJob) {
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (CollUtil.isEmpty(tenantIds)) {
            return "无可执行的租户";
        }
        Map<Long, String> results = new ConcurrentHashMap<>();
        for (Long tenantId : tenantIds) {
            TenantUtils.execute(tenantId, () -> {
                try {
                    Object result = joinPoint.proceed();
                    results.put(tenantId, result == null ? "OK" : result.toString());
                } catch (Throwable e) {
                    log.error("[around] 租户 {} 的任务执行失败", tenantId, e);
                    results.put(tenantId, "异常: " + e.getMessage());
                }
            });
        }
        return results.toString();
    }
}
