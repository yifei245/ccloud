package com.ccloud.module.system.controller.admin.oauth2.vo.token;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 访问令牌分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OAuth2AccessTokenPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "1")
    private Long userId;

    @Schema(description = "用户类型：1 会员 2 管理员", example = "2")
    private Integer userType;

    @Schema(description = "客户端编号，模糊匹配", example = "ccloud-web")
    private String clientId;
}
