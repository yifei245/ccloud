package com.ccloud.module.pay.support;

import com.ccloud.module.pay.framework.pay.core.client.PayClientConfig;
import lombok.Data;

import java.util.Set;

/**
 * 测试用渠道配置，校验为空操作
 */
@Data
public class FakePayClientConfig implements PayClientConfig {

    private String secret;

    @Override
    public void validate() {
    }

    @Override
    public Set<String> sensitiveFields() {
        return Set.of("secret");
    }

}
