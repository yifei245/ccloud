package com.ccloud.module.system.controller.admin.permission.vo.permission;

import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.module.system.enums.permission.DataScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Collections;
import java.util.Set;

@Schema(description = "管理后台 - 赋予角色数据权限 Request VO")
@Data
public class PermissionAssignRoleDataScopeReqVO {

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "角色编号不能为空")
    private Long roleId;

    @Schema(description = "数据范围：1 全部 2 指定部门 3 本部门 4 本部门及以下 5 仅本人",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "数据范围不能为空")
    @InEnum(value = DataScopeEnum.class, message = "数据范围必须是 {value}")
    private Integer dataScope;

    @Schema(description = "部门编号列表，数据范围为指定部门时必填", example = "1,3,5")
    private Set<Long> dataScopeDeptIds = Collections.emptySet();
}
