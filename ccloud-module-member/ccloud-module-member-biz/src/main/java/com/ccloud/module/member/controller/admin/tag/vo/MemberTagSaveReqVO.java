package com.ccloud.module.member.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 会员标签创建/修改 Request VO")
@Data
public class MemberTagSaveReqVO {

    @Schema(description = "标签编号，修改时必填", example = "1")
    private Long id;

    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "高价值")
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称长度不能超过 50 个字符")
    private String name;
}
