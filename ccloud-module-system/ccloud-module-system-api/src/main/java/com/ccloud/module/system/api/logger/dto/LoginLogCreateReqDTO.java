package com.ccloud.module.system.api.logger.dto;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.validation.InEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建登录日志请求
 */
@Data
public class LoginLogCreateReqDTO implements Serializable {

    /**
     * 日志类型
     */
    @NotNull(message = "日志类型不能为空")
    private Integer logType;

    /**
     * 链路追踪编号
     */
    private String traceId;

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 用户类型
     */
    @NotNull(message = "用户类型不能为空")
    @InEnum(UserTypeEnum.class)
    private Integer userType;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 登录结果
     */
    @NotNull(message = "登录结果不能为空")
    private Integer result;

    /**
     * 用户 IP
     */
    @NotEmpty(message = "用户 IP 不能为空")
    private String userIp;

    /**
     * 浏览器 UA
     */
    private String userAgent;
}
