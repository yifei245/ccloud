package com.ccloud.module.open.controller.admin.app.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 开放应用分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OpenAppPageReqVO extends PageParam {

    @Schema(description = "应用名称，模糊匹配", example = "合作方")
    private String name;

    @Schema(description = "应用标识，精确匹配", example = "a1b2c3d4e5f6g7h8")
    private String appKey;

    @Schema(description = "状态，对应字典 common_status", example = "0")
    private Integer status;
}
