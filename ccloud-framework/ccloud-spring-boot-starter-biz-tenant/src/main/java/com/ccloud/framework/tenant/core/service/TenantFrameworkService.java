package com.ccloud.framework.tenant.core.service;

import java.util.List;

/**
 * 租户信息查询契约。实现由 system 模块经 RPC 提供，框架层只依赖接口。
 */
public interface TenantFrameworkService {

    /**
     * @return 所有开启且未过期的租户编号，供定时任务逐租户执行
     */
    List<Long> getTenantIds();

    /**
     * 校验租户是否可用。不存在、已禁用、已过期分别抛出不同的业务异常。
     *
     * @throws com.ccloud.framework.common.exception.ServiceException 租户不可用时抛出
     */
    void validTenant(Long id);

    /**
     * @return 是否为系统租户（套餐编号 0，不受套餐限制）。租户不存在返回 false
     */
    boolean isSystemTenant(Long id);
}
