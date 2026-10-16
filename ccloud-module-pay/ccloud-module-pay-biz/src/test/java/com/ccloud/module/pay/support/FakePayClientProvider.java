package com.ccloud.module.pay.support;

import com.ccloud.module.pay.enums.PayChannelEnum;
import com.ccloud.module.pay.framework.pay.core.client.PayClient;
import com.ccloud.module.pay.framework.pay.core.client.PayClientConfig;
import com.ccloud.module.pay.framework.pay.core.client.PayClientProvider;

/**
 * 测试用渠道，编码 mock。所有渠道编号共用同一个客户端实例，测试经 client() 设置返回值
 */
public class FakePayClientProvider implements PayClientProvider {

    private final FakePayClient client = new FakePayClient();

    public FakePayClient client() {
        return client;
    }

    @Override public String getCode() { return PayChannelEnum.MOCK.getCode(); }
    @Override public Class<? extends PayClientConfig> getConfigClass() { return FakePayClientConfig.class; }

    @Override
    public PayClient create(Long channelId, PayClientConfig config) {
        client.channelId = channelId;
        return client;
    }

}
