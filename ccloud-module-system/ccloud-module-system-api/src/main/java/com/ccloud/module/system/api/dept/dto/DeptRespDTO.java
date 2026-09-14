package com.ccloud.module.system.api.dept.dto;


import lombok.Data;

import java.io.Serializable;

/**
 * 部门
 */
@Data
public class DeptRespDTO implements Serializable {

    /**
     * 部门编号
     */
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门编号
     */
    private Long parentId;

    /**
     * 负责人用户编号
     */
    private Long leaderUserId;

    /**
     * 状态
     */
    private Integer status;
}
