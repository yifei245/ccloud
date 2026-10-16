package com.ccloud.module.pay.controller.admin.order.vo;

import com.ccloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 支付单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PayOrderPageReqVO extends PageParam {

    @Schema(description = "应用编号", example = "1024")
    private Long appId;

    @Schema(description = "渠道编码", example = "mock")
    private String channelCode;

    @Schema(description = "商户订单号", example = "ORDER-20261012-0001")
    private String merchantOrderId;

    @Schema(description = "支付成功的外部订单号", example = "P20261012153000000001")
    private String no;

    @Schema(description = "支付状态，对应字典 pay_order_status", example = "10")
    private Integer status;

    @Schema(description = "创建时间区间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
