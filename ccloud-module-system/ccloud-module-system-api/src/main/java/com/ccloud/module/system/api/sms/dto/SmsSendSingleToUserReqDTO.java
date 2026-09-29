package com.ccloud.module.system.api.sms.dto;

import com.ccloud.framework.common.validation.Mobile;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import lombok.Data;

import java.io.Serializable;

/**
 * 给单个用户发送短信请求
 */
@Data
public class SmsSendSingleToUserReqDTO implements Serializable {

    /**
     * 用户编号，为空时直接按手机号发送
     */
    private Long userId;

    /**
     * 手机号，为空时按用户编号查询
     */
    @Mobile
    private String mobile;

    /**
     * 短信模板编码
     */
    @NotEmpty(message = "短信模板编码不能为空")
    private String templateCode;

    /**
     * 短信模板参数
     */
    private Map<String, Object> templateParams;
}
