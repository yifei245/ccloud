package com.ccloud.module.member.controller.admin.user.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 会员用户分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MemberUserPageReqVO extends PageParam {

    @Schema(description = "手机号，模糊匹配", example = "138")
    private String mobile;

    @Schema(description = "昵称，模糊匹配", example = "张")
    private String nickname;

    @Schema(description = "注册时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;

    @Schema(description = "最后登录时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] loginDate;

    @Schema(description = "等级编号", example = "2")
    private Long levelId;

    @Schema(description = "分组编号", example = "1")
    private Long groupId;

    @Schema(description = "标签编号，命中任一即可", example = "1,2")
    private List<Long> tagIds;
}
