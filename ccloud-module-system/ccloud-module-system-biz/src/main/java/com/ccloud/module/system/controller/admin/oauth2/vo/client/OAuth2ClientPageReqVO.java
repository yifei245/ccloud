package com.ccloud.module.system.controller.admin.oauth2.vo.client;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - OAuth2 客户端分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OAuth2ClientPageReqVO extends PageParam {

    @Schema(description = "应用名，模糊匹配", example = "管理后台")
    private String name;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;
}
