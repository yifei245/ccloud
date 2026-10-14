package com.ccloud.module.pay.framework.pay.core.client.impl.wallet;

import com.ccloud.module.pay.enums.PayChannelEnum;
import com.ccloud.module.pay.framework.pay.core.client.PayClient;
import com.ccloud.module.pay.framework.pay.core.client.PayClientConfig;
import com.ccloud.module.pay.framework.pay.core.client.PayClientProvider;
import com.ccloud.module.pay.service.wallet.PayWalletService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class WalletPayClientProvider implements PayClientProvider {

    @Resource
    private PayWalletService payWalletService;

    @Override
    public String getCode() {
        return PayChannelEnum.WALLET.getCode();
    }

    @Override
    public Class<? extends PayClientConfig> getConfigClass() {
        return WalletPayClientConfig.class;
    }

    @Override
    public PayClient create(Long channelId, PayClientConfig config) {
        return new WalletPayClient(channelId, payWalletService);
    }
}
