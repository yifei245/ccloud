package com.ccloud.module.member.framework.rpc.config;

import com.ccloud.module.infra.api.file.FileApi;
import com.ccloud.module.system.api.dict.DictDataApi;
import com.ccloud.module.system.api.i18n.I18nMessageApi;
import com.ccloud.module.system.api.logger.LoginLogApi;
import com.ccloud.module.system.api.logger.OperateLogApi;
import com.ccloud.module.system.api.oauth2.OAuth2TokenApi;
import com.ccloud.module.system.api.permission.PermissionApi;
import com.ccloud.module.system.api.sms.SmsCodeApi;
import com.ccloud.module.system.api.social.SocialClientApi;
import com.ccloud.module.system.api.social.SocialUserApi;
import com.ccloud.module.system.api.tenant.TenantApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * member 调用 system 的 Feign 客户端，逐个显式列出。
 * member-api 自身的接口不注册为客户端：本服务内由本地 RestController 实现
 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {
        // 框架契约：权限、租户、字典、国际化、操作日志
        PermissionApi.class, TenantApi.class, DictDataApi.class, I18nMessageApi.class, OperateLogApi.class,
        // C 端认证：令牌、登录日志、短信验证码、社交登录
        OAuth2TokenApi.class, LoginLogApi.class, SmsCodeApi.class, SocialClientApi.class, SocialUserApi.class,
        // 文件：头像
        FileApi.class
})
public class RpcConfiguration {
}
