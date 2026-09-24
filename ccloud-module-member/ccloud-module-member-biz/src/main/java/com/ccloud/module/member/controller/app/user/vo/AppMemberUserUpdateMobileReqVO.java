package com.ccloud.module.member.controller.app.user.vo;

import com.ccloud.framework.common.validation.Mobile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "用户 App - 修改手机号 Request VO。新旧手机号都要验证，只验新号会让拿到登录态的人直接换绑账号")
@Data
public class AppMemberUserUpdateMobileReqVO {

    @Schema(description = "新手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13900139000")
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

    @Schema(description = "新手机号收到的验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "验证码不能为空")
    private String code;

    @Schema(description = "原手机号收到的验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "654321")
    @NotBlank(message = "原手机验证码不能为空")
    private String oldCode;
}
