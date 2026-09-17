package com.ccloud.module.system.controller.admin.oauth2.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "OAuth2 开放接口 - 授权页信息 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2OpenAuthorizeInfoRespVO {

    @Schema(description = "客户端")
    private Client client;

    @Schema(description = "申请的授权范围及用户此前是否已同意")
    private List<Scope> scopes;

    @Schema(description = "客户端")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Client {

        @Schema(description = "应用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "管理后台")
        private String name;

        @Schema(description = "应用图标", example = "https://www.ccloud.local/logo.png")
        private String logo;
    }

    @Schema(description = "授权范围")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scope {

        @Schema(description = "授权范围", requiredMode = Schema.RequiredMode.REQUIRED, example = "user.read")
        private String key;

        @Schema(description = "是否已同意", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        private Boolean value;
    }
}
