package com.ccloud.framework.test.core.util;

import cn.hutool.core.util.ReflectUtil;
import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 断言工具
 */
public class AssertUtils {

    /**
     * 逐字段比对两个对象。
     *
     * @param ignoreFields 需要忽略的字段，如自增主键、创建时间这类不可预测的值
     */
    public static void assertPojoEquals(Object expected, Object actual, String... ignoreFields) {
        List<String> ignored = Arrays.asList(ignoreFields);
        Field[] fields = ReflectUtil.getFields(expected.getClass());
        for (Field field : fields) {
            if (ignored.contains(field.getName()) || field.isSynthetic()) {
                continue;
            }
            Object expectedValue = ReflectUtil.getFieldValue(expected, field);
            Object actualValue = ReflectUtil.getFieldValue(actual, field);
            Assertions.assertTrue(Objects.equals(expectedValue, actualValue),
                    () -> String.format("字段 %s 不一致：期望 %s，实际 %s",
                            field.getName(), expectedValue, actualValue));
        }
    }
}
