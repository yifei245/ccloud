package com.ccloud.module.system.api.permission.dto;

import java.util.Set;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门数据权限。字段与 data-permission starter 中的同名 DTO 一致，单独定义以免调用方被拖入持久层依赖
 */
@Data
public class DeptDataPermissionRespDTO implements Serializable {

    /**
     * 是否可查看全部数据
     */
    private Boolean all;

    /**
     * 可查看的部门编号集合
     */
    private Set<Long> deptIds;

    /**
     * 是否可查看本人数据
     */
    private Boolean self;
}
