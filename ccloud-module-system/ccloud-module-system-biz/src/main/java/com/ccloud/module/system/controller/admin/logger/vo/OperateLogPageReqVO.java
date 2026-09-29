package com.ccloud.module.system.controller.admin.logger.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 操作日志分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OperateLogPageReqVO extends PageParam {

    @Schema(description = "操作人编号", example = "1")
    private Long userId;

    @Schema(description = "操作模块，模糊匹配", example = "用户管理")
    private String type;

    @Schema(description = "操作名，模糊匹配", example = "修改用户")
    private String subType;

    @Schema(description = "业务数据编号", example = "1024")
    private Long bizId;

    @Schema(description = "操作内容，模糊匹配", example = "修改")
    private String action;

    @Schema(description = "是否成功", example = "true")
    private Boolean success;

    @Schema(description = "操作时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
