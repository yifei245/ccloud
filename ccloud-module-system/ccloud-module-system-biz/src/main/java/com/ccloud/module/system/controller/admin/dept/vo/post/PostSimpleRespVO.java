package com.ccloud.module.system.controller.admin.dept.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 岗位精简信息 Response VO")
@Data
public class PostSimpleRespVO {

    @Schema(description = "岗位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "董事长")
    private String name;
}
