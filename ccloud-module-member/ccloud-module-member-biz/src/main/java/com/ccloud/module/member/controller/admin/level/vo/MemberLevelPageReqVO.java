package com.ccloud.module.member.controller.admin.level.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 会员等级分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MemberLevelPageReqVO extends PageParam {

    @Schema(description = "等级名称，模糊匹配", example = "白银")
    private String name;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;
}
