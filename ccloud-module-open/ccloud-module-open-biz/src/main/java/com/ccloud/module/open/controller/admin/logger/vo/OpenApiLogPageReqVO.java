package com.ccloud.module.open.controller.admin.logger.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 开放接口调用日志分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OpenApiLogPageReqVO extends PageParam {

    @Schema(description = "应用编号", example = "1024")
    private Long appId;

    @Schema(description = "应用标识，精确匹配", example = "a1b2c3d4e5f6g7h8")
    private String appKey;

    @Schema(description = "接口编码，精确匹配", example = "member.user.get")
    private String apiCode;

    @Schema(description = "业务结果码", example = "0")
    private Integer resultCode;

    @Schema(description = "开始时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] beginTime;
}
