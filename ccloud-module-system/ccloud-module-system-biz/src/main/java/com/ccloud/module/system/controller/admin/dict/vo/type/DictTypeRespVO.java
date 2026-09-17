package com.ccloud.module.system.controller.admin.dict.vo.type;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.excel.core.annotations.DictFormat;
import com.ccloud.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 字典类型 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DictTypeRespVO {

    @Schema(description = "字典类型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("字典主键")
    private Long id;

    @Schema(description = "字典名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "性别")
    @ExcelProperty("字典名称")
    private String name;

    @Schema(description = "字典类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "system_user_sex")
    @ExcelProperty("字典类型")
    private String type;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(CommonStatusEnum.DICT_TYPE)
    private Integer status;

    @Schema(description = "备注", example = "用户性别")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
