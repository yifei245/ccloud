package com.ccloud.module.system.controller.admin.mail.vo.account;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 邮箱账号分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MailAccountPageReqVO extends PageParam {

    @Schema(description = "邮箱，模糊匹配", example = "noreply")
    private String mail;

    @Schema(description = "用户名，模糊匹配", example = "noreply")
    private String username;
}
