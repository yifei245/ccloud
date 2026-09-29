package com.ccloud.framework.common.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 键值对。用于需要携带一对关联数据、又不值得单开一个类的场景。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class KeyValue<K, V> implements Serializable {

    private K key;
    private V value;
}
