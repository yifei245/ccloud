package com.ccloud.module.member.controller.admin.point.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员积分记录分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MemberPointRecordPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "1")
    private Long userId;

    @Schema(description = "业务类型，见字典 member_point_biz_type", example = "1")
    private Integer bizType;

    @Schema(description = "标题，模糊匹配", example = "签到")
    private String title;

    @Schema(description = "创建时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
