package com.ccloud.module.infra.controller.admin.config.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 参数配置分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ConfigPageReqVO extends PageParam {

    @Schema(description = "参数名称，模糊匹配", example = "注册开关")
    private String name;

    @Schema(description = "参数键名，模糊匹配", example = "system.auth")
    private String configKey;

    @Schema(description = "参数类型，对应字典 infra_config_type", example = "1")
    private Integer type;

    @Schema(description = "创建时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
