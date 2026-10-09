package com.ccloud.module.infra.controller.admin.mq.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 消息死信分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MessageDeadLetterPageReqVO extends PageParam {

    @Schema(description = "消息主题，模糊匹配", example = "order.created")
    private String topic;

    @Schema(description = "消息编号，精确匹配", example = "6f1c0c7e8a7b4d3f9a1b2c3d4e5f6a7b")
    private String messageId;

    @Schema(description = "是否已人工处理", example = "false")
    private Boolean handled;

    @Schema(description = "创建时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
