package com.ccloud.module.system.controller.admin.permission.vo.role;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 角色状态修改 Request VO")
@Data
public class RoleUpdateStatusReqVO {

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "角色编号不能为空")
    private Long id;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;
}
