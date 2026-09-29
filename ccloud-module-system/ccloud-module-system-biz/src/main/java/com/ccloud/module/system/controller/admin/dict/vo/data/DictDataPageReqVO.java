package com.ccloud.module.system.controller.admin.dict.vo.data;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 字典数据分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DictDataPageReqVO extends PageParam {

    @Schema(description = "字典标签，模糊匹配", example = "男")
    private String label;

    @Schema(description = "字典类型", example = "system_user_sex")
    private String dictType;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;
}
