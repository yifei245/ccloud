package com.ccloud.module.member.controller.admin.tag.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员标签分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MemberTagPageReqVO extends PageParam {

    @Schema(description = "标签名称，模糊匹配", example = "价值")
    private String name;

    @Schema(description = "创建时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
