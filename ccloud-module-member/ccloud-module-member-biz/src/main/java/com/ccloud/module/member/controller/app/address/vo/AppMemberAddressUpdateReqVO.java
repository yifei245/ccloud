package com.ccloud.module.member.controller.app.address.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "用户 App - 收件地址修改 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppMemberAddressUpdateReqVO extends AppMemberAddressCreateReqVO {

    @Schema(description = "地址编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "地址编号不能为空")
    private Long id;
}
