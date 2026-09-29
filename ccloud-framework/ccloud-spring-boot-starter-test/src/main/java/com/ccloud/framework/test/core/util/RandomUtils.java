package com.ccloud.framework.test.core.util;

import cn.hutool.core.util.RandomUtil;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.util.function.Consumer;

/**
 * 测试数据生成。
 *
 * <p>用随机值填充对象，好处是：测试不会意外依赖某个"恰好合适"的固定值，
 * 断言必须显式声明自己关心的字段。
 */
public class RandomUtils {

    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();

    public static String randomString() {
        return RandomUtil.randomString(10);
    }

    public static Long randomLongId() {
        return RandomUtil.randomLong(1, 100000);
    }

    public static Integer randomInteger() {
        return RandomUtil.randomInt(1, 100);
    }

    /**
     * 生成随机填充的对象
     *
     * @param consumers 用于覆盖测试真正关心的字段
     */
    @SafeVarargs
    public static <T> T randomPojo(Class<T> clazz, Consumer<T>... consumers) {
        T pojo = PODAM_FACTORY.manufacturePojo(clazz);
        for (Consumer<T> consumer : consumers) {
            consumer.accept(pojo);
        }
        return pojo;
    }
}
