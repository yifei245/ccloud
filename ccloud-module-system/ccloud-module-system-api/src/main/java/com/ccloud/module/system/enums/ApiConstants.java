package com.ccloud.module.system.enums;

import com.ccloud.framework.common.enums.RpcConstants;

/**
 * system 模块的 RPC 常量
 */
public interface ApiConstants {

    /**
     * 服务名，与 spring.application.name 及 Nacos 注册名一致
     */
    String NAME = "ccloud-system-server";

    /**
     * RPC 路径前缀。写在每个方法的映射注解上而非 {@code @FeignClient.path}：
     * 服务端实现类实现接口时会继承方法上的映射，却不会继承 path，
     * 用 path 会造成调用方与服务端路径不一致。
     */
    String PREFIX = RpcConstants.RPC_API_PREFIX + "/system";

    String VERSION = "1.0.0";
}
