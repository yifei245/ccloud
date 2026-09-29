package com.ccloud.module.system.controller.admin.logger.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 登录日志分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LoginLogPageReqVO extends PageParam {

    @Schema(description = "用户账号，模糊匹配", example = "admin")
    private String username;

    @Schema(description = "用户 IP，模糊匹配", example = "10.0.0.1")
    private String userIp;

    @Schema(description = "日志类型：100 账号登录 101 社交登录 103 短信登录 200 主动登出 202 强制登出", example = "100")
    private Integer logType;

    @Schema(description = "是否登录成功", example = "true")
    private Boolean status;

    @Schema(description = "登录时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
