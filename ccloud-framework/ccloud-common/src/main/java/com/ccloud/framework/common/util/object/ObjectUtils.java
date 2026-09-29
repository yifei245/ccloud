package com.ccloud.framework.common.util.object;

import cn.hutool.core.util.ArrayUtil;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 对象工具
 */
public class ObjectUtils {

    /**
     * 返回第一个非空的值；全为空时返回 null
     */
    @SafeVarargs
    public static <T> T defaultIfNull(T... array) {
        return ArrayUtil.isEmpty(array) ? null
                : Arrays.stream(array).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @SafeVarargs
    public static <T> boolean equalsAny(T obj, T... array) {
        return !ArrayUtil.isEmpty(array) && Arrays.asList(array).contains(obj);
    }

    public static <T extends Comparable<T>> T max(T a, T b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static <T extends Comparable<T>> T min(T a, T b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) <= 0 ? a : b;
    }

    /**
     * 克隆并按比较器重排。用于不希望污染原列表的场景。
     */
    public static <T> List<T> cloneSorted(List<T> source, Comparator<T> comparator) {
        if (source == null) {
            return null;
        }
        return source.stream().sorted(comparator).toList();
    }
}
