package com.ccloud.framework.common.core;

/**
 * 可生成 int 数组的接口。枚举实现它之后，可配合 {@code @InEnum} 做取值校验。
 */
public interface IntArrayValuable {

    /**
     * @return 该枚举全部合法取值组成的数组
     */
    int[] array();
}
