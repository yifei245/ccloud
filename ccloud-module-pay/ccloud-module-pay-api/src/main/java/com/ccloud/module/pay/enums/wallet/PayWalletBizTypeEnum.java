package com.ccloud.module.pay.enums.wallet;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 钱包流水业务类型，字典 pay_wallet_biz_type
 */
@Getter
@AllArgsConstructor
public enum PayWalletBizTypeEnum implements IntArrayValuable {

    RECHARGE(1, "充值"),
    RECHARGE_REFUND(2, "充值退款"),
    PAYMENT(3, "支付"),
    PAYMENT_REFUND(4, "支付退款"),
    /** 本期不用，保留与字典一致 */
    WITHDRAW(5, "提现"),
    ADJUST(6, "后台调整"),
    TRANSFER_IN(7, "转账入账");

    public static final String DICT_TYPE = "pay_wallet_biz_type";

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(PayWalletBizTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
}
