package com.ccloud.module.member.controller.admin.group.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员分组 Response VO")
@Data
public class MemberGroupRespVO {

    @Schema(description = "分组编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "普通会员")
    private String name;

    @Schema(description = "备注", example = "线下门店注册的会员")
    private String remark;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
