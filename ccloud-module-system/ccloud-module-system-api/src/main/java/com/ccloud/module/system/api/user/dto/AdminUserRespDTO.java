package com.ccloud.module.system.api.user.dto;

import java.util.Set;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理后台用户
 */
@Data
public class AdminUserRespDTO implements Serializable {

    /**
     * 用户编号
     */
    private Long id;

    /**
     * 账号
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 部门编号
     */
    private Long deptId;

    /**
     * 岗位编号数组
     */
    private Set<Long> postIds;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 头像
     */
    private String avatar;
}
