package com.ccloud.module.pay.framework.pay.core.client.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一下单请求
 */
@Data
@Accessors(chain = true)
public class PayOrderUnifiedReq {

    /** 支付尝试编号，即外部单号 */
    private String outTradeNo;
    private String subject;
    private String body;
    /** 单位分 */
    private Integer price;
    private LocalDateTime expireTime;
    /** 渠道回调地址 */
    private String notifyUrl;
    private String userIp;
    private String payMethod;
    private Long userId;
    private Integer userType;
    /** 渠道特有参数，如微信小程序的 openid */
    private Map<String, String> channelExtras;

}
