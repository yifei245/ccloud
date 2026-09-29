package com.ccloud.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 用户导入结果 Response VO")
@Data
public class UserImportRespVO {

    @Schema(description = "创建成功的用户账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createUsernames = new ArrayList<>();

    @Schema(description = "更新成功的用户账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> updateUsernames = new ArrayList<>();

    @Schema(description = "导入失败的用户账号，key 为账号，value 为失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureUsernames = new LinkedHashMap<>();
}
