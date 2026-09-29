package com.ccloud.module.system.controller.admin.dept.vo.dept;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 部门列表 Request VO")
@Data
public class DeptListReqVO {

    @Schema(description = "部门名称，模糊匹配", example = "研发")
    private String name;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;
}
