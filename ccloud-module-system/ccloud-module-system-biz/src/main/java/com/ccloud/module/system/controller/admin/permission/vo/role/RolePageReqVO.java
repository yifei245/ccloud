package com.ccloud.module.system.controller.admin.permission.vo.role;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 角色分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RolePageReqVO extends PageParam {

    @Schema(description = "角色名称，模糊匹配", example = "运营")
    private String name;

    @Schema(description = "角色标识，模糊匹配", example = "operator")
    private String code;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;

    @Schema(description = "创建时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
