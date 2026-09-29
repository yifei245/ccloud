package com.ccloud.module.system.service.tenant;

import com.ccloud.module.system.dal.dataobject.tenant.TenantDO;

/**
 * 以当前租户信息执行逻辑，如校验账号配额
 */
@FunctionalInterface
public interface TenantInfoHandler {

    void handle(TenantDO tenant);
}
