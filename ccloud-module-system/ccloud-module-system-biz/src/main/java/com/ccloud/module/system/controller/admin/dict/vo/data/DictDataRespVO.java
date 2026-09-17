package com.ccloud.module.system.controller.admin.dict.vo.data;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.excel.core.annotations.DictFormat;
import com.ccloud.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 字典数据 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DictDataRespVO {

    @Schema(description = "字典数据编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("字典编码")
    private Long id;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("字典排序")
    private Integer sort;

    @Schema(description = "字典标签", requiredMode = Schema.RequiredMode.REQUIRED, example = "男")
    @ExcelProperty("字典标签")
    private String label;

    @Schema(description = "字典值", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("字典键值")
    private String value;

    @Schema(description = "字典类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "system_user_sex")
    @ExcelProperty("字典类型")
    private String dictType;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(CommonStatusEnum.DICT_TYPE)
    private Integer status;

    @Schema(description = "颜色类型", example = "primary")
    private String colorType;

    @Schema(description = "CSS 样式", example = "btn-visible")
    private String cssClass;

    @Schema(description = "备注", example = "性别男")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
