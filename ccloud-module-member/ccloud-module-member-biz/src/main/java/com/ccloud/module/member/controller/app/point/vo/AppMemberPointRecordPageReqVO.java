package com.ccloud.module.member.controller.app.point.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "用户 App - 积分记录分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppMemberPointRecordPageReqVO extends PageParam {

    @Schema(description = "true 只看收入，false 只看支出，不传看全部", example = "true")
    private Boolean addStatus;
}
