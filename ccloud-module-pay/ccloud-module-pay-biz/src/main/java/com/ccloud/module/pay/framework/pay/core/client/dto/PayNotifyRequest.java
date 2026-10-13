package com.ccloud.module.pay.framework.pay.core.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 渠道回调的原始请求，供验签与解析
 */
@Data
@AllArgsConstructor
public class PayNotifyRequest {

    private Map<String, String> params;
    /** 请求头，键为小写 */
    private Map<String, String> headers;
    private String body;

}
