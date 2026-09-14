package com.ccloud.module.system.api.dept.dto;


import lombok.Data;

import java.io.Serializable;

/**
 * 岗位
 */
@Data
public class PostRespDTO implements Serializable {

    /**
     * 岗位编号
     */
    private Long id;

    /**
     * 岗位名称
     */
    private String name;

    /**
     * 岗位编码
     */
    private String code;

    /**
     * 状态
     */
    private Integer status;
}
