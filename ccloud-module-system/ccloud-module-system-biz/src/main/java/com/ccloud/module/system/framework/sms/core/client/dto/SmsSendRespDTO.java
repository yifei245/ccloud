package com.ccloud.module.system.framework.sms.core.client.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 渠道受理短信的结果。受理成功不代表用户已收到，送达结果由渠道回执另行通知
 */
@Data
@Accessors(chain = true)
public class SmsSendRespDTO {

    private Boolean success;

    /**
     * 渠道返回的结果码
     */
    private String apiCode;

    /**
     * 渠道返回的结果说明
     */
    private String apiMsg;

    /**
     * 渠道请求编号，排查问题时提供给渠道方
     */
    private String apiRequestId;

    /**
     * 渠道侧的短信流水号，用于关联回执
     */
    private String serialNo;
}
