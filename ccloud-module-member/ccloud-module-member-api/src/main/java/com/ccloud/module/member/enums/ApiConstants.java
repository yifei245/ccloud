package com.ccloud.module.member.enums;

import com.ccloud.framework.common.enums.RpcConstants;

/**
 * member 模块的 RPC 常量
 */
public interface ApiConstants {

    /**
     * 服务名，与 spring.application.name 及 Nacos 注册名一致
     */
    String NAME = "ccloud-member-server";

    /**
     * RPC 路径前缀，写在每个方法的映射注解上（原因见 system 模块的 ApiConstants）
     */
    String PREFIX = RpcConstants.RPC_API_PREFIX + "/member";

    String VERSION = "1.0.0";
}
