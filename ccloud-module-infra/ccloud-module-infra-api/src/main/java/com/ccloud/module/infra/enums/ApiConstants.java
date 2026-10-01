package com.ccloud.module.infra.enums;

import com.ccloud.framework.common.enums.RpcConstants;

/**
 * infra 模块的 RPC 常量
 */
public interface ApiConstants {

    /**
     * 服务名，与 spring.application.name 及 Nacos 注册名一致
     */
    String NAME = "ccloud-infra-server";

    /**
     * RPC 路径前缀，写在每个方法的映射注解上（原因见 system 模块的 ApiConstants）
     */
    String PREFIX = RpcConstants.RPC_API_PREFIX + "/infra";

    String VERSION = "1.0.0";
}
