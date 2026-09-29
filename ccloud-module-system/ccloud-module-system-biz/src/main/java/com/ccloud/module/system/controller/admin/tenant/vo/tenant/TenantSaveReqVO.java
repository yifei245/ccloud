package com.ccloud.module.system.controller.admin.tenant.vo.tenant;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.framework.common.validation.Mobile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 租户创建/修改 Request VO")
@Data
public class TenantSaveReqVO {

    @Schema(description = "租户编号，修改时必填", example = "1")
    private Long id;

    @Schema(description = "租户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud")
    @NotBlank(message = "租户名不能为空")
    @Size(max = 30, message = "租户名长度不能超过 30 个字符")
    private String name;

    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "联系人不能为空")
    @Size(max = 30, message = "联系人长度不能超过 30 个字符")
    private String contactName;

    @Schema(description = "联系手机", example = "13800138000")
    @Mobile
    private String contactMobile;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "租户状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "绑定域名", example = "tenant.ccloud.local")
    @Size(max = 256, message = "绑定域名长度不能超过 256 个字符")
    private String website;

    @Schema(description = "租户套餐编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "租户套餐编号不能为空")
    private Long packageId;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "过期时间不能为空")
    private LocalDateTime expireTime;

    @Schema(description = "账号数量上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "账号数量上限不能为空")
    @Min(value = 1, message = "账号数量上限至少为 1")
    private Integer accountCount;

    // ========== 仅创建时使用：租户管理员账号 ==========

    @Schema(description = "租户管理员账号，创建时必填", example = "tenantadmin")
    private String username;

    @Schema(description = "租户管理员密码，创建时必填", example = "admin123")
    private String password;

    @AssertTrue(message = "管理员账号必须为 4-30 位字母或数字")
    @JsonIgnore
    public boolean isUsernameValid() {
        return id != null || (StrUtil.isNotEmpty(username) && username.matches("^[a-zA-Z0-9]{4,30}$"));
    }

    @AssertTrue(message = "管理员密码长度必须为 4-16 位")
    @JsonIgnore
    public boolean isPasswordValid() {
        return id != null || (StrUtil.isNotEmpty(password) && password.length() >= 4 && password.length() <= 16);
    }
}
