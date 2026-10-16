package com.ccloud.module.pay.framework.pay.core.client;

import java.util.Set;

/**
 * 渠道配置，对应支付渠道的 config 字段（JSON）
 */
public interface PayClientConfig {

    /** 配置不合法时抛 IllegalArgumentException，message 作为提示 */
    void validate();

    /** 敏感字段（JSON 属性名），后台回显为 *** */
    default Set<String> sensitiveFields() {
        return Set.of();
    }

}
