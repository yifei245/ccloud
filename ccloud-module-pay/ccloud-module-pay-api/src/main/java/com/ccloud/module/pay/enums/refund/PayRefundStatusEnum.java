package com.ccloud.module.pay.enums.refund;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 退款状态，字典 pay_refund_status
 */
@Getter
@AllArgsConstructor
public enum PayRefundStatusEnum implements IntArrayValuable {

    WAITING(0, "未退款"),
    SUCCESS(10, "退款成功"),
    FAILURE(20, "退款失败");

    public static final String DICT_TYPE = "pay_refund_status";

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(PayRefundStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
}
