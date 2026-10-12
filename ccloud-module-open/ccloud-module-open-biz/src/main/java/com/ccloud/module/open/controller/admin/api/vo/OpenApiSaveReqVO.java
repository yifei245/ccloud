package com.ccloud.module.open.controller.admin.api.vo;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 开放接口创建/修改 Request VO")
@Data
@Accessors(chain = true)
public class OpenApiSaveReqVO {

    @Schema(description = "接口编号，修改时必填", example = "1024")
    private Long id;

    @Schema(description = "接口名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "查询会员")
    @NotBlank(message = "接口名称不能为空")
    @Size(max = 64, message = "接口名称长度不能超过 64 个字符")
    private String name;

    @Schema(description = "接口编码，全局唯一", requiredMode = Schema.RequiredMode.REQUIRED, example = "member.user.get")
    @NotBlank(message = "接口编码不能为空")
    @Size(max = 128, message = "接口编码长度不能超过 128 个字符")
    @Pattern(regexp = "[a-z][a-z0-9_.-]*", message = "接口编码须以小写字母开头，只能包含小写字母、数字、下划线、点和中划线")
    private String code;

    @Schema(description = "接口路径，/open-api/ 开头", requiredMode = Schema.RequiredMode.REQUIRED, example = "/open-api/member/user/get")
    @NotBlank(message = "接口路径不能为空")
    @Size(max = 255, message = "接口路径长度不能超过 255 个字符")
    private String path;

    @Schema(description = "请求方法，不区分大小写，保存为大写", requiredMode = Schema.RequiredMode.REQUIRED, example = "GET")
    @NotBlank(message = "请求方法不能为空")
    @Pattern(regexp = "(?i)GET|POST|PUT|DELETE", message = "请求方法只能是 GET、POST、PUT、DELETE")
    private String method;

    @Schema(description = "接口描述", example = "按编号查询会员")
    @Size(max = 512, message = "接口描述长度不能超过 512 个字符")
    private String description;

    @Schema(description = "状态，对应字典 common_status", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;
}
