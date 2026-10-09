package com.ccloud.module.member.controller.admin.group.vo;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 会员分组创建/修改 Request VO")
@Data
public class MemberGroupSaveReqVO {

    @Schema(description = "分组编号，修改时必填", example = "1")
    private Long id;

    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "普通会员")
    @NotBlank(message = "分组名称不能为空")
    @Size(max = 50, message = "分组名称长度不能超过 50 个字符")
    private String name;

    @Schema(description = "备注", example = "线下门店注册的会员")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;
}
