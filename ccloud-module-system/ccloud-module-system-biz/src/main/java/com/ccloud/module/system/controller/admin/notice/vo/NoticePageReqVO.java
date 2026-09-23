package com.ccloud.module.system.controller.admin.notice.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 通知公告分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NoticePageReqVO extends PageParam {

    @Schema(description = "公告标题，模糊匹配", example = "升级")
    private String title;

    @Schema(description = "状态：0 开启 1 关闭", example = "0")
    private Integer status;
}
