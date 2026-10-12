package com.ccloud.module.open.controller.admin.logger.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出时不含请求参数：体积大，且 Excel 单元格放不下
 */
@Schema(description = "管理后台 - 开放接口调用日志 Response VO")
@Data
@ExcelIgnoreUnannotated
public class OpenApiLogRespVO {

    @Schema(description = "日志编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("日志编号")
    private Long id;

    @Schema(description = "租户编号", example = "1")
    @ExcelProperty("租户编号")
    private Long tenantId;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("应用编号")
    private Long appId;

    @Schema(description = "应用标识，应用已删除时为空", example = "a1b2c3d4e5f6g7h8")
    @ExcelProperty("应用标识")
    private String appKey;

    @Schema(description = "接口编码，请求不在接口目录中时为空", example = "member.user.get")
    @ExcelProperty("接口编码")
    private String apiCode;

    @Schema(description = "请求方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "GET")
    @ExcelProperty("请求方法")
    private String requestMethod;

    @Schema(description = "请求地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "/open-api/member/user/get")
    @ExcelProperty("请求地址")
    private String requestUrl;

    @Schema(description = "请求参数，敏感字段已脱敏")
    private String requestParams;

    @Schema(description = "业务结果码", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("结果码")
    private Integer resultCode;

    @Schema(description = "结果提示", example = "成功")
    @ExcelProperty("结果提示")
    private String resultMsg;

    @Schema(description = "调用方 IP", requiredMode = Schema.RequiredMode.REQUIRED, example = "127.0.0.1")
    @ExcelProperty("调用方 IP")
    private String userIp;

    @Schema(description = "执行时长，单位毫秒", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @ExcelProperty("执行时长")
    private Integer duration;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("开始时间")
    private LocalDateTime beginTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("结束时间")
    private LocalDateTime endTime;

    @Schema(description = "链路追踪编号", example = "66600cb6-7852-11eb-9439-0242ac130002")
    @ExcelProperty("链路追踪编号")
    private String traceId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
