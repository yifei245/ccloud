package com.ccloud.module.system.controller.admin.logger.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 操作日志 Response VO")
@Data
@ExcelIgnoreUnannotated
public class OperateLogRespVO {

    @Schema(description = "日志编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("日志编号")
    private Long id;

    @Schema(description = "链路追踪编号", example = "89aca178-a370-411c-ae02-3f0d672be4ab")
    private String traceId;

    @Schema(description = "操作人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("操作人编号")
    private Long userId;

    @Schema(description = "用户类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer userType;

    @Schema(description = "操作模块", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户管理")
    @ExcelProperty("操作模块")
    private String type;

    @Schema(description = "操作名", requiredMode = Schema.RequiredMode.REQUIRED, example = "修改用户")
    @ExcelProperty("操作名")
    private String subType;

    @Schema(description = "业务数据编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("业务编号")
    private Long bizId;

    @Schema(description = "操作内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "修改用户")
    @ExcelProperty("操作内容")
    private String action;

    @Schema(description = "是否成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @ExcelProperty("是否成功")
    private Boolean success;

    @Schema(description = "拓展字段 JSON：操作类型、参数、返回值、耗时", example = "{\"duration\":3}")
    private String extra;

    @Schema(description = "请求方法", example = "PUT")
    @ExcelProperty("请求方法")
    private String requestMethod;

    @Schema(description = "请求地址", example = "/admin-api/system/user/update")
    @ExcelProperty("请求地址")
    private String requestUrl;

    @Schema(description = "用户 IP", example = "127.0.0.1")
    @ExcelProperty("用户 IP")
    private String userIp;

    @Schema(description = "浏览器 UA", example = "Mozilla/5.0")
    private String userAgent;

    @Schema(description = "操作时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("操作时间")
    private LocalDateTime createTime;
}
