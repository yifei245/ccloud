package com.ccloud.module.pay.framework.rpc.config;

import com.ccloud.module.open.api.app.OpenAppApi;
import com.ccloud.module.open.api.notify.OpenNotifyApi;
import com.ccloud.module.system.api.dict.DictDataApi;
import com.ccloud.module.system.api.i18n.I18nMessageApi;
import com.ccloud.module.system.api.logger.OperateLogApi;
import com.ccloud.module.system.api.permission.PermissionApi;
import com.ccloud.module.system.api.tenant.TenantApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * pay 调用其他服务的 Feign 客户端，逐个显式列出。
 * pay-api 自身的接口不注册为客户端：本服务内由本地 RestController 实现
 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {
        // 框架契约：权限、租户、字典、国际化、操作日志
        PermissionApi.class, TenantApi.class, DictDataApi.class, I18nMessageApi.class, OperateLogApi.class,
        // 开放平台：绑定开放应用时校验归属；向外部系统发送支付回调
        OpenAppApi.class, OpenNotifyApi.class
})
public class RpcConfiguration {
}
