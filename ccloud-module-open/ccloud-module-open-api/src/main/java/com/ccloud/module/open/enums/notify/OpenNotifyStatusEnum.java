package com.ccloud.module.open.enums.notify;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 回调状态，字典 open_notify_status
 */
@Getter
@AllArgsConstructor
public enum OpenNotifyStatusEnum implements IntArrayValuable {

    WAITING(0, "待通知"),
    SUCCESS(10, "成功"),
    FAILURE(20, "失败待重试"),
    ABANDONED(30, "放弃");

    public static final String DICT_TYPE = "open_notify_status";

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(OpenNotifyStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
}
