package com.ccloud.module.infra.enums.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 参数类型，对应字典 infra_config_type
 */
@Getter
@AllArgsConstructor
public enum ConfigTypeEnum {

    /**
     * 系统内置：代码依赖其键名，不能删除、不能改键名
     */
    SYSTEM(1),
    CUSTOM(2);

    private final Integer type;
}
