package com.ccloud.module.system.api.logger.dto;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.validation.InEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建操作日志请求
 */
@Data
public class OperateLogCreateReqDTO implements Serializable {

    /**
     * 链路追踪编号
     */
    private String traceId;

    /**
     * 用户编号
     */
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    /**
     * 用户类型
     */
    @NotNull(message = "用户类型不能为空")
    @InEnum(UserTypeEnum.class)
    private Integer userType;

    /**
     * 操作模块类型
     */
    @NotEmpty(message = "操作模块类型不能为空")
    private String type;

    /**
     * 操作名
     */
    @NotEmpty(message = "操作名不能为空")
    private String subType;

    /**
     * 操作数据模块编号
     */
    @NotNull(message = "操作数据模块编号不能为空")
    private Long bizId;

    /**
     * 操作内容
     */
    @NotEmpty(message = "操作内容不能为空")
    private String action;

    /**
     * 操作结果
     */
    private Boolean success;

    /**
     * 拓展字段，JSON
     */
    private String extra;

    /**
     * 请求方法名
     */
    private String requestMethod;

    /**
     * 请求地址
     */
    private String requestUrl;

    /**
     * 用户 IP
     */
    private String userIp;

    /**
     * 浏览器 UA
     */
    private String userAgent;
}
