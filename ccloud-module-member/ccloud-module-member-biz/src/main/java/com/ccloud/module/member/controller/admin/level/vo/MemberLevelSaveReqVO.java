package com.ccloud.module.member.controller.admin.level.vo;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 会员等级创建/修改 Request VO")
@Data
public class MemberLevelSaveReqVO {

    @Schema(description = "等级编号，修改时必填", example = "1")
    private Long id;

    @Schema(description = "等级名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "白银会员")
    @NotBlank(message = "等级名称不能为空")
    @Size(max = 30, message = "等级名称长度不能超过 30 个字符")
    private String name;

    @Schema(description = "等级序号，数值越大等级越高", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "等级序号不能为空")
    @Min(value = 1, message = "等级序号必须大于 0")
    private Integer level;

    @Schema(description = "升级所需经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    @NotNull(message = "升级所需经验不能为空")
    @Min(value = 0, message = "升级所需经验不能小于 0")
    private Integer experience;

    @Schema(description = "享受折扣百分比，100 表示不打折", requiredMode = Schema.RequiredMode.REQUIRED, example = "98")
    @NotNull(message = "折扣不能为空")
    @Min(value = 0, message = "折扣不能小于 0")
    @Max(value = 100, message = "折扣不能大于 100")
    private Integer discountPercent;

    @Schema(description = "等级图标", example = "https://ccloud.local/level/2.png")
    @Size(max = 255, message = "等级图标地址长度不能超过 255 个字符")
    private String icon;

    @Schema(description = "等级背景图", example = "https://ccloud.local/level/2-bg.png")
    @Size(max = 255, message = "等级背景图地址长度不能超过 255 个字符")
    private String backgroundUrl;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;
}
