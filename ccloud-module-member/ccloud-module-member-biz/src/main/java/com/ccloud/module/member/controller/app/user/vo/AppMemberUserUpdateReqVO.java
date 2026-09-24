package com.ccloud.module.member.controller.app.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Schema(description = "用户 App - 修改基本信息 Request VO，只更新非空字段")
@Data
public class AppMemberUserUpdateReqVO {

    @Schema(description = "昵称", example = "芋艿")
    @Size(max = 30, message = "昵称不能超过 30 个字符")
    private String nickname;

    @Schema(description = "头像", example = "https://www.ccloud.local/avatar.png")
    @Size(max = 512, message = "头像地址不能超过 512 个字符")
    @URL(message = "头像必须是 URL")
    private String avatar;

    @Schema(description = "性别：0 未知 1 男 2 女", example = "1")
    @Min(value = 0, message = "性别取值为 0-2")
    @Max(value = 2, message = "性别取值为 0-2")
    private Integer sex;

    @Schema(description = "生日")
    private LocalDateTime birthday;
}
