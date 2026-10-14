package com.ccloud.module.pay.api.wallet;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.ccloud.module.pay.dal.mysql.wallet.PayWalletMapper;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class PayWalletApiImpl implements PayWalletApi {

    @Resource
    private PayWalletMapper walletMapper;

    /**
     * 只读查询，不为查询开通钱包：不存在按 0 返回
     */
    @Override
    public CommonResult<Integer> getWalletBalance(Long userId, Integer userType) {
        PayWalletDO wallet = walletMapper.selectByUser(userId, userType);
        return success(wallet == null ? 0 : wallet.getBalance());
    }
}
