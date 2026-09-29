package com.ccloud.framework.common.core;

/**
 * 可生成 T 数组的接口。枚举实现它之后，可配合 {@code @InEnum} 做取值校验。
 *
 * @param <T> 数组元素类型
 */
public interface ArrayValuable<T> {

    /**
     * @return 该枚举全部合法取值组成的数组
     */
    T[] array();
}
