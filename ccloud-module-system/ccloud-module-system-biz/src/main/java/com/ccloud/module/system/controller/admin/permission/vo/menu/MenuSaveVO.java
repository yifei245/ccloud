package com.ccloud.module.system.controller.admin.permission.vo.menu;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.module.system.enums.permission.MenuTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 菜单创建/修改 Request VO")
@Data
public class MenuSaveVO {

    @Schema(description = "菜单编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户管理")
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过 50 个字符")
    private String name;

    @Schema(description = "权限标识，仅按钮类型需要", example = "system:user:create")
    @Size(max = 100, message = "权限标识长度不能超过 100 个字符")
    private String permission;

    @Schema(description = "类型：1 目录 2 菜单 3 按钮", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "菜单类型不能为空")
    @InEnum(value = MenuTypeEnum.class, message = "菜单类型必须是 {value}")
    private Integer type;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "显示顺序不能为空")
    private Integer sort;

    @Schema(description = "父菜单编号，0 为顶级", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "父菜单编号不能为空")
    private Long parentId;

    @Schema(description = "路由地址，目录与菜单需要", example = "user")
    @Size(max = 200, message = "路由地址长度不能超过 200 个字符")
    private String path;

    @Schema(description = "菜单图标", example = "ep:user")
    private String icon;

    @Schema(description = "组件路径，菜单需要", example = "system/user/index")
    @Size(max = 255, message = "组件路径长度不能超过 255 个字符")
    private String component;

    @Schema(description = "组件名", example = "SystemUser")
    private String componentName;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "是否可见", example = "true")
    private Boolean visible;

    @Schema(description = "是否缓存", example = "true")
    private Boolean keepAlive;

    @Schema(description = "是否总是显示", example = "false")
    private Boolean alwaysShow;
}
