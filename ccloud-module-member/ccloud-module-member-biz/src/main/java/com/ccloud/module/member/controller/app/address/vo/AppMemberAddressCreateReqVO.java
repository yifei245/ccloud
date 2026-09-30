package com.ccloud.module.member.controller.app.address.vo;

import com.ccloud.framework.common.validation.Mobile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "用户 App - 收件地址创建 Request VO")
@Data
public class AppMemberAddressCreateReqVO {

    @Schema(description = "收件人名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "收件人名称不能为空")
    @Size(max = 10, message = "收件人名称长度不能超过 10 个字符")
    private String name;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

    @Schema(description = "地区编号，由前端按区划数据选择", requiredMode = Schema.RequiredMode.REQUIRED, example = "440305")
    @NotNull(message = "地区不能为空")
    private Long areaId;

    @Schema(description = "详细地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "科技园 1 号")
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 250, message = "详细地址长度不能超过 250 个字符")
    private String detailAddress;

    @Schema(description = "是否设为默认地址；第一个地址无论如何都会设为默认", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否默认地址不能为空")
    private Boolean defaultStatus;
}
