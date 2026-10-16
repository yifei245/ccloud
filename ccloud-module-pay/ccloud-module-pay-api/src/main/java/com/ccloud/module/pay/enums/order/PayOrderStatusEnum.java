package com.ccloud.module.pay.enums.order;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 支付单状态。支付尝试（pay_order_extension）复用 WAITING、SUCCESS、CLOSED 三个值，字典 pay_order_status
 */
@Getter
@AllArgsConstructor
public enum PayOrderStatusEnum implements IntArrayValuable {

    WAITING(0, "待支付"),
    SUCCESS(10, "支付成功"),
    REFUND(20, "已退款"),
    CLOSED(30, "已关闭");

    public static final String DICT_TYPE = "pay_order_status";

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(PayOrderStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
}
