package com.ccloud.module.open.controller.admin.api.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 开放接口分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OpenApiPageReqVO extends PageParam {

    @Schema(description = "接口名称，模糊匹配", example = "会员")
    private String name;

    @Schema(description = "接口编码，模糊匹配", example = "member")
    private String code;

    @Schema(description = "接口路径，模糊匹配", example = "/open-api/member")
    private String path;

    @Schema(description = "状态，对应字典 common_status", example = "0")
    private Integer status;
}
