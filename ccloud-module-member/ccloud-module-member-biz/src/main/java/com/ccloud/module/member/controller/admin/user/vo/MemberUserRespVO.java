package com.ccloud.module.member.controller.admin.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 会员用户 Response VO")
@Data
public class MemberUserRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String mobile;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "注册 IP", requiredMode = Schema.RequiredMode.REQUIRED, example = "127.0.0.1")
    private String registerIp;

    @Schema(description = "注册终端，见 TerminalEnum", example = "31")
    private Integer registerTerminal;

    @Schema(description = "最后登录 IP", example = "127.0.0.1")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "真实姓名", example = "张三")
    private String name;

    @Schema(description = "性别：0 未知 1 男 2 女", example = "1")
    private Integer sex;

    @Schema(description = "出生日期")
    private LocalDateTime birthday;

    @Schema(description = "所在地区编号", example = "440305")
    private Integer areaId;

    @Schema(description = "运营备注", example = "重点客户")
    private String mark;

    @Schema(description = "积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer point;

    @Schema(description = "经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1200")
    private Integer experience;

    @Schema(description = "等级编号", example = "2")
    private Long levelId;

    @Schema(description = "等级名称", example = "白银会员")
    private String levelName;

    @Schema(description = "分组编号", example = "1")
    private Long groupId;

    @Schema(description = "分组名称", example = "普通会员")
    private String groupName;

    @Schema(description = "标签编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1, 2]")
    private List<Long> tagIds;

    @Schema(description = "注册时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
}
