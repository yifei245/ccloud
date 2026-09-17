package com.ccloud.module.system.controller.admin.user.vo.user;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 用户分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserPageReqVO extends PageParam {

    @Schema(description = "用户账号，模糊匹配", example = "admin")
    private String username;

    @Schema(description = "手机号码，模糊匹配", example = "1380013")
    private String mobile;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;

    @Schema(description = "创建时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;

    @Schema(description = "部门编号，同时查询其全部子部门", example = "100")
    private Long deptId;
}
