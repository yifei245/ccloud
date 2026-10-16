package com.ccloud.module.pay.controller.admin.app.vo;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 支付应用创建/修改 Request VO")
@Data
@Accessors(chain = true)
public class PayAppSaveReqVO {

    @Schema(description = "应用编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "应用标识，创建后不可修改，更新时忽略", requiredMode = Schema.RequiredMode.REQUIRED, example = "mall-shop")
    @NotBlank(message = "应用标识不能为空")
    @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{1,63}$", message = "应用标识只能包含小写字母、数字、下划线与中划线，2 到 64 位")
    private String appKey;

    @Schema(description = "应用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "商城")
    @NotBlank(message = "应用名不能为空")
    @Size(max = 64, message = "应用名长度不能超过 64 个字符")
    private String name;

    @Schema(description = "状态，对应字典 common_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "备注", example = "商城订单支付")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "绑定的开放应用标识；为空表示不绑定（不对外开放）", example = "a1b2c3d4e5f6g7h8")
    @Size(max = 64, message = "开放应用标识长度不能超过 64 个字符")
    private String openAppKey;
}
