package com.ccloud.module.system.controller.admin.social.vo.client;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 社交客户端分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SocialClientPageReqVO extends PageParam {

    @Schema(description = "应用名，模糊匹配", example = "钉钉")
    private String name;

    @Schema(description = "社交平台", example = "20")
    private Integer socialType;

    @Schema(description = "用户类型", example = "2")
    private Integer userType;

    @Schema(description = "应用编号，模糊匹配", example = "ding")
    private String clientId;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;
}
