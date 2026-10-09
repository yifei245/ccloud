package com.ccloud.module.infra.controller.admin.logger.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - API 异常日志分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ApiErrorLogPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "666")
    private Long userId;

    @Schema(description = "用户类型，对应字典 user_type", example = "1")
    private Integer userType;

    @Schema(description = "应用名，精确匹配", example = "ccloud-system-server")
    private String applicationName;

    @Schema(description = "请求地址，模糊匹配", example = "/admin-api/system/user")
    private String requestUrl;

    @Schema(description = "异常发生时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] exceptionTime;

    @Schema(description = "处理状态：0 未处理 1 已处理 2 已忽略", example = "0")
    private Integer processStatus;
}
