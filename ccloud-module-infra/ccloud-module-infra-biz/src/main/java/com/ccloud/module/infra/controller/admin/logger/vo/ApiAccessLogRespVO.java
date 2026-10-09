package com.ccloud.module.infra.controller.admin.logger.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ccloud.framework.excel.core.annotations.DictFormat;
import com.ccloud.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出时不含请求参数与响应结果：体积大，且 Excel 单元格放不下
 */
@Schema(description = "管理后台 - API 访问日志 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ApiAccessLogRespVO {

    @Schema(description = "日志主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("日志主键")
    private Long id;

    @Schema(description = "链路追踪编号", example = "66600cb6-7852-11eb-9439-0242ac130002")
    @ExcelProperty("链路追踪编号")
    private String traceId;

    @Schema(description = "租户编号", example = "1")
    @ExcelProperty("租户编号")
    private Long tenantId;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "666")
    @ExcelProperty("用户编号")
    private Long userId;

    @Schema(description = "用户类型，对应字典 user_type", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty(value = "用户类型", converter = DictConvert.class)
    @DictFormat("user_type")
    private Integer userType;

    @Schema(description = "应用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud-system-server")
    @ExcelProperty("应用名")
    private String applicationName;

    @Schema(description = "请求方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "GET")
    @ExcelProperty("请求方法")
    private String requestMethod;

    @Schema(description = "请求地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "/admin-api/system/user/page")
    @ExcelProperty("请求地址")
    private String requestUrl;

    @Schema(description = "请求参数")
    private String requestParams;

    @Schema(description = "响应结果")
    private String responseBody;

    @Schema(description = "用户 IP", requiredMode = Schema.RequiredMode.REQUIRED, example = "127.0.0.1")
    @ExcelProperty("用户 IP")
    private String userIp;

    @Schema(description = "浏览器 UA", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("浏览器 UA")
    private String userAgent;

    @Schema(description = "操作模块", example = "管理后台 - 用户")
    @ExcelProperty("操作模块")
    private String operateModule;

    @Schema(description = "操作名", example = "导出用户")
    @ExcelProperty("操作名")
    private String operateName;

    @Schema(description = "操作分类，对应字典 system_operate_type", example = "1")
    @ExcelProperty(value = "操作分类", converter = DictConvert.class)
    @DictFormat("system_operate_type")
    private Integer operateType;

    @Schema(description = "开始请求时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("开始请求时间")
    private LocalDateTime beginTime;

    @Schema(description = "结束请求时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("结束请求时间")
    private LocalDateTime endTime;

    @Schema(description = "执行时长，单位毫秒", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @ExcelProperty("执行时长")
    private Integer duration;

    @Schema(description = "业务结果码", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("结果码")
    private Integer resultCode;

    @Schema(description = "结果提示", example = "成功")
    @ExcelProperty("结果提示")
    private String resultMsg;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
