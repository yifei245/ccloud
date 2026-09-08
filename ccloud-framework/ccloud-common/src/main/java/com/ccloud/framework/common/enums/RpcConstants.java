package com.ccloud.framework.common.enums;

/**
 * RPC 相关常量
 */
public interface RpcConstants {

    /**
     * 内部 RPC 接口的统一前缀。网关会拦截该前缀的外部请求，直接返回 403。
     */
    String RPC_API_PREFIX = "/rpc-api";

    /**
     * 管理端接口前缀
     */
    String ADMIN_API_PREFIX = "/admin-api";

    /**
     * 用户端接口前缀
     */
    String APP_API_PREFIX = "/app-api";

    /**
     * 开放平台接口前缀
     */
    String OPEN_API_PREFIX = "/open-api";
}
