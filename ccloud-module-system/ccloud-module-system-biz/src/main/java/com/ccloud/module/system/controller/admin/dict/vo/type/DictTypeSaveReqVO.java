package com.ccloud.module.system.controller.admin.dict.vo.type;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 字典类型创建/修改 Request VO")
@Data
public class DictTypeSaveReqVO {

    @Schema(description = "字典类型编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "字典名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "性别")
    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称长度不能超过 100 个字符")
    private String name;

    @Schema(description = "字典类型，创建后不可修改", requiredMode = Schema.RequiredMode.REQUIRED, example = "system_user_sex")
    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型长度不能超过 100 个字符")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "字典类型必须以小写字母开头，且只能包含小写字母、数字、下划线")
    private String type;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "备注", example = "用户性别")
    private String remark;
}
