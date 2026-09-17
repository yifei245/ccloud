package com.ccloud.module.system.controller.admin.permission.vo.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 菜单列表 Request VO")
@Data
public class MenuListReqVO {

    @Schema(description = "菜单名称，模糊匹配", example = "用户")
    private String name;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;
}
