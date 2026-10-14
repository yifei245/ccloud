package com.ccloud.module.pay.api.wallet;

import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.ccloud.module.pay.dal.mysql.wallet.PayWalletMapper;
import com.ccloud.module.pay.test.BasePayDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PayWalletApiImpl.class)
class PayWalletApiImplTest extends BasePayDbUnitTest {

    @Resource
    private PayWalletApiImpl walletApi;
    @Resource
    private PayWalletMapper walletMapper;

    @Test
    void 钱包不存在时余额按0返回_且不开通钱包() {
        assertThat(walletApi.getWalletBalance(72001L, 1).getData()).isZero();
        assertThat(walletMapper.selectByUser(72001L, 1)).isNull();
    }

    @Test
    void 钱包存在时返回余额() {
        walletMapper.insert(new PayWalletDO().setUserId(72002L).setUserType(1)
                .setBalance(321).setTotalExpense(0).setTotalRecharge(0).setFreezePrice(0));
        assertThat(walletApi.getWalletBalance(72002L, 1).getData()).isEqualTo(321);
    }
}
