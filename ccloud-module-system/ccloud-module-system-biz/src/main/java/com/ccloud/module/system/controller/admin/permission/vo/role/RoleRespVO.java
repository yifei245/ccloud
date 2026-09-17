package com.ccloud.module.system.controller.admin.permission.vo.role;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.excel.core.annotations.DictFormat;
import com.ccloud.framework.excel.core.convert.DictConvert;
import com.ccloud.module.system.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "管理后台 - 角色 Response VO")
@Data
@ExcelIgnoreUnannotated
public class RoleRespVO {

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("角色序号")
    private Long id;

    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "运营")
    @ExcelProperty("角色名称")
    private String name;

    @Schema(description = "角色标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "operator")
    @ExcelProperty("角色标志")
    private String code;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("角色排序")
    private Integer sort;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "角色状态", converter = DictConvert.class)
    @DictFormat(CommonStatusEnum.DICT_TYPE)
    private Integer status;

    @Schema(description = "角色类型：1 内置 2 自定义", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer type;

    @Schema(description = "备注", example = "负责日常运营")
    private String remark;

    @Schema(description = "数据范围：1 全部 2 指定部门 3 本部门 4 本部门及以下 5 仅本人", example = "1")
    @ExcelProperty(value = "数据范围", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.DATA_SCOPE)
    private Integer dataScope;

    @Schema(description = "数据范围为指定部门时的部门编号", example = "[1, 2]")
    private Set<Long> dataScopeDeptIds;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
