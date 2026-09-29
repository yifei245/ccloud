package com.ccloud.module.system.api.dict.dto;


import lombok.Data;

import java.io.Serializable;

/**
 * 字典数据
 */
@Data
public class DictDataRespDTO implements Serializable {

    /**
     * 字典标签
     */
    private String label;

    /**
     * 字典值
     */
    private String value;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 状态
     */
    private Integer status;
}
