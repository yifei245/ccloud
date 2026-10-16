package com.ccloud.module.pay.enums.transfer;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 转账状态，字典 pay_transfer_status
 */
@Getter
@AllArgsConstructor
public enum PayTransferStatusEnum implements IntArrayValuable {

    WAITING(0, "等待转账"),
    PROCESSING(5, "转账进行中"),
    SUCCESS(10, "转账成功"),
    FAILURE(20, "转账失败");

    public static final String DICT_TYPE = "pay_transfer_status";

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(PayTransferStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
}
