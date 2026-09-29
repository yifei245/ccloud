package com.ccloud.framework.common.enums;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 时间间隔类型，用于统计报表按维度聚合
 */
@Getter
@AllArgsConstructor
public enum DateIntervalEnum implements IntArrayValuable {

    DAY(1, "天"),
    WEEK(2, "周"),
    MONTH(3, "月"),
    QUARTER(4, "季度"),
    YEAR(5, "年");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(DateIntervalEnum::getInterval).toArray();

    private final Integer interval;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static DateIntervalEnum valueOf(Integer interval) {
        return Arrays.stream(values())
                .filter(e -> Objects.equals(e.getInterval(), interval))
                .findFirst()
                .orElse(null);
    }
}
