package com.ccloud.framework.common.util.string;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * 字符串工具
 */
public class StrUtils {

    /**
     * 截断并追加省略号。用于日志、错误信息等不希望无限长的场景。
     */
    public static String maxLength(CharSequence str, int maxLength) {
        return StrUtil.maxLength(str, maxLength - 3);
    }

    /**
     * 判断 str 是否以 prefixes 中任意一个开头
     */
    public static boolean startWithAny(String str, Collection<String> prefixes) {
        if (StrUtil.isEmpty(str) || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        return prefixes.stream().anyMatch(str::startsWith);
    }

    public static List<Long> splitToLong(String value, CharSequence separator) {
        long[] longs = StrUtil.splitToLong(value, separator);
        return java.util.Arrays.stream(longs).boxed().toList();
    }

    public static List<Integer> splitToInteger(String value, CharSequence separator) {
        int[] ints = StrUtil.splitToInt(value, separator);
        return java.util.Arrays.stream(ints).boxed().toList();
    }

    /**
     * 脱敏：保留首尾各 keep 个字符，中间用 * 填充
     */
    public static String desensitize(String str, int keepPrefix, int keepSuffix) {
        if (StrUtil.isBlank(str)) {
            return str;
        }
        if (str.length() <= keepPrefix + keepSuffix) {
            return StringUtils.repeat('*', str.length());
        }
        return str.substring(0, keepPrefix)
                + StringUtils.repeat('*', str.length() - keepPrefix - keepSuffix)
                + str.substring(str.length() - keepSuffix);
    }

    /**
     * 手机号脱敏：138****8000
     */
    public static String desensitizeMobile(String mobile) {
        return desensitize(mobile, 3, 4);
    }
}
