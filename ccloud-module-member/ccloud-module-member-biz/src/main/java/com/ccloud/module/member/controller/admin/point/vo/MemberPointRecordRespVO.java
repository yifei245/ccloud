package com.ccloud.module.member.controller.admin.point.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员积分记录 Response VO")
@Data
public class MemberPointRecordRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "业务类型，见字典 member_point_biz_type", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer bizType;

    @Schema(description = "业务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private String bizId;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "签到")
    private String title;

    @Schema(description = "描述", example = "签到获得 2 积分")
    private String description;

    @Schema(description = "变动积分，负数表示扣减", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer point;

    @Schema(description = "变动后的积分余额", requiredMode = Schema.RequiredMode.REQUIRED, example = "102")
    private Integer totalPoint;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
