package com.ccloud.module.infra.controller.admin.logger.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - API 访问日志分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ApiAccessLogPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "666")
    private Long userId;

    @Schema(description = "用户类型，对应字典 user_type", example = "1")
    private Integer userType;

    @Schema(description = "应用名，精确匹配", example = "ccloud-system-server")
    private String applicationName;

    @Schema(description = "请求地址，模糊匹配", example = "/admin-api/system/user")
    private String requestUrl;

    @Schema(description = "开始请求时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] beginTime;

    @Schema(description = "执行时长下限，单位毫秒", example = "100")
    private Integer duration;

    @Schema(description = "业务结果码", example = "0")
    private Integer resultCode;
}
