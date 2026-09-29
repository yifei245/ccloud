package com.ccloud.module.system.controller.admin.user.vo.user;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.validation.Mobile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Schema(description = "管理后台 - 用户创建/修改 Request VO")
@Data
public class UserSaveReqVO {

    @Schema(description = "用户编号，修改时必填", example = "1")
    private Long id;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    @NotBlank(message = "用户账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$", message = "用户账号由 4-30 位数字或字母组成")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @Size(max = 30, message = "用户昵称长度不能超过 30 个字符")
    private String nickname;

    @Schema(description = "备注", example = "新员工")
    private String remark;

    @Schema(description = "部门编号", example = "100")
    private Long deptId;

    @Schema(description = "岗位编号数组", example = "[1, 2]")
    private Set<Long> postIds;

    @Schema(description = "邮箱", example = "zhangsan@ccloud.local")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    private String email;

    @Schema(description = "手机号码", example = "13800138000")
    @Mobile
    private String mobile;

    @Schema(description = "用户性别：0 未知 1 男 2 女", example = "1")
    private Integer sex;

    @Schema(description = "头像地址", example = "https://www.ccloud.local/avatar.png")
    private String avatar;

    @Schema(description = "密码，创建时必填", example = "123456")
    private String password;

    @AssertTrue(message = "密码长度必须为 4-16 位")
    @JsonIgnore
    public boolean isPasswordValid() {
        return id != null || (StrUtil.isNotEmpty(password) && password.length() >= 4 && password.length() <= 16);
    }
}
