package com.ccloud.module.open.controller.admin.app.vo;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 开放应用创建/修改 Request VO")
@Data
@Accessors(chain = true)
public class OpenAppSaveReqVO {

    @Schema(description = "应用编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "合作方 A")
    @NotBlank(message = "应用名称不能为空")
    @Size(max = 64, message = "应用名称长度不能超过 64 个字符")
    private String name;

    @Schema(description = "状态，对应字典 common_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "备注", example = "对接订单系统")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "IP 白名单，逗号分隔的 IPv4/IPv6 地址或 CIDR，为空表示不限制", example = "10.0.0.0/8,203.0.113.7")
    @Size(max = 1024, message = "IP 白名单长度不能超过 1024 个字符")
    private String ipWhitelist;

    @Schema(description = "每秒请求上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "每秒请求上限不能为空")
    @Min(value = 1, message = "每秒请求上限不能小于 1")
    @Max(value = 10000, message = "每秒请求上限不能大于 10000")
    private Integer rateLimit;

    @Schema(description = "过期时间，为空表示长期有效")
    private LocalDateTime expireTime;

    @Schema(description = "联系人", example = "张三")
    @Size(max = 64, message = "联系人长度不能超过 64 个字符")
    private String contactName;

    @Schema(description = "联系电话", example = "13800000000")
    @Size(max = 20, message = "联系电话长度不能超过 20 个字符")
    private String contactMobile;

    @Schema(description = "事件回调地址，http 或 https 开头", example = "https://example.com/notify")
    @Size(max = 512, message = "回调地址长度不能超过 512 个字符")
    private String notifyUrl;
}
