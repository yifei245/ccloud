package com.ccloud.module.system.controller.admin.user.vo.profile;

import com.ccloud.framework.common.validation.Mobile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 修改个人资料 Request VO")
@Data
public class UserProfileUpdateReqVO {

    @Schema(description = "用户昵称", example = "管理员")
    @Size(max = 30, message = "用户昵称长度不能超过 30 个字符")
    private String nickname;

    @Schema(description = "用户邮箱", example = "admin@ccloud.local")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    private String email;

    @Schema(description = "手机号码", example = "13800138000")
    @Mobile
    private String mobile;

    @Schema(description = "用户性别：0 未知 1 男 2 女", example = "1")
    private Integer sex;

    @Schema(description = "头像地址", example = "https://www.ccloud.local/avatar.png")
    @Size(max = 512, message = "头像地址长度不能超过 512 个字符")
    private String avatar;
}
