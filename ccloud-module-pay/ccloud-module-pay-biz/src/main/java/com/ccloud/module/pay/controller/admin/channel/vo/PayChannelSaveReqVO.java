package com.ccloud.module.pay.controller.admin.channel.vo;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 支付渠道创建/修改 Request VO")
@Data
@ToString(exclude = "config")
@Accessors(chain = true)
public class PayChannelSaveReqVO {

    @Schema(description = "渠道编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "应用编号，修改时忽略", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "应用编号不能为空")
    private Long appId;

    @Schema(description = "渠道编码，修改时忽略", requiredMode = Schema.RequiredMode.REQUIRED, example = "mock")
    @NotBlank(message = "渠道编码不能为空")
    private String code;

    @Schema(description = "状态，对应字典 common_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "渠道费率，单位百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.6")
    @NotNull(message = "渠道费率不能为空")
    @DecimalMin(value = "0", message = "渠道费率不能小于 0")
    @DecimalMax(value = "100", message = "渠道费率不能大于 100")
    private Double feeRate;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "渠道配置 JSON；修改时敏感字段提交 *** 表示保持原值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "渠道配置不能为空")
    @Size(max = 2000, message = "渠道配置不能超过 2000 个字符")
    private String config;
}
