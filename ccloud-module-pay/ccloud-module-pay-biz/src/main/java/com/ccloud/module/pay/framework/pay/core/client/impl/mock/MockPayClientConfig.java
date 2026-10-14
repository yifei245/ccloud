package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

import cn.hutool.core.util.StrUtil;
import com.ccloud.module.pay.framework.pay.core.client.PayClientConfig;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@Accessors(chain = true)
public class MockPayClientConfig implements PayClientConfig {

    /** 回调签名密钥，至少 16 位 */
    private String notifySecret;
    private MockPayBehavior behavior = MockPayBehavior.SUCCESS;
    /** DELAY 行为的延迟秒数 */
    private Integer delaySeconds = 3;

    @Override
    public void validate() {
        if (StrUtil.length(notifySecret) < 16) {
            throw new IllegalArgumentException("notifySecret 至少 16 位");
        }
        if (behavior == null) {
            throw new IllegalArgumentException("behavior 不能为空");
        }
        if (delaySeconds == null || delaySeconds < 1 || delaySeconds > 60) {
            throw new IllegalArgumentException("delaySeconds 须在 1 到 60 之间");
        }
    }

    @Override
    public Set<String> sensitiveFields() {
        return Set.of("notifySecret");
    }
}
