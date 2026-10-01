package com.ccloud.module.infra.framework.rpc.config;

import com.ccloud.module.system.api.dict.DictDataApi;
import com.ccloud.module.system.api.i18n.I18nMessageApi;
import com.ccloud.module.system.api.logger.OperateLogApi;
import com.ccloud.module.system.api.permission.PermissionApi;
import com.ccloud.module.system.api.tenant.TenantApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * infra 调用 system 的 Feign 客户端，逐个显式列出。
 * infra-api 自身的接口不注册为客户端：本服务内由本地 RestController 实现
 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {
        // 框架契约：权限、租户、字典、国际化、操作日志
        PermissionApi.class, TenantApi.class, DictDataApi.class, I18nMessageApi.class, OperateLogApi.class
})
public class RpcConfiguration {
}
