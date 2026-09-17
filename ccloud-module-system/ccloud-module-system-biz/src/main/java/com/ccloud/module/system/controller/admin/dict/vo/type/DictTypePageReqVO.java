package com.ccloud.module.system.controller.admin.dict.vo.type;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 字典类型分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DictTypePageReqVO extends PageParam {

    @Schema(description = "字典名称，模糊匹配", example = "性别")
    private String name;

    @Schema(description = "字典类型，模糊匹配", example = "system_user_sex")
    private String type;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;

    @Schema(description = "创建时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
