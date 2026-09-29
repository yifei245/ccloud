package com.ccloud.framework.common.enums;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 通用状态枚举。全项目的 status 字段统一使用该语义：0 开启，1 关闭。
 */
@Getter
@AllArgsConstructor
public enum CommonStatusEnum implements IntArrayValuable {

    ENABLE(0, "开启"),
    DISABLE(1, "关闭");

    public static final String DICT_TYPE = "common_status";

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(CommonStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static boolean isEnable(Integer status) {
        return Objects.equals(ENABLE.status, status);
    }

    public static boolean isDisable(Integer status) {
        return Objects.equals(DISABLE.status, status);
    }
}
