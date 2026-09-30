package com.ccloud.module.member.controller.admin.user.vo;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 会员用户修改 Request VO")
@Data
public class MemberUserUpdateReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "用户编号不能为空")
    private Long id;

    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过 30 个字符")
    private String nickname;

    @Schema(description = "头像", example = "https://ccloud.local/avatar.png")
    @Size(max = 512, message = "头像地址长度不能超过 512 个字符")
    private String avatar;

    @Schema(description = "状态：0 开启 1 关闭；关闭后该会员的令牌全部作废", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "性别：0 未知 1 男 2 女", example = "1")
    private Integer sex;

    @Schema(description = "出生日期")
    private LocalDateTime birthday;

    @Schema(description = "运营备注", example = "重点客户")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String mark;

    @Schema(description = "标签编号，不传或传空列表表示清空", example = "[1, 2]")
    private List<Long> tagIds;

    @Schema(description = "分组编号，不传表示清空；须为启用的分组", example = "1")
    private Long groupId;
}
