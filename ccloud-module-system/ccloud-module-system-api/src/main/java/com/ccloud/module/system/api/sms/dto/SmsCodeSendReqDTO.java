package com.ccloud.module.system.api.sms.dto;

import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.framework.common.validation.Mobile;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 发送短信验证码请求
 */
@Data
public class SmsCodeSendReqDTO implements Serializable {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

    /**
     * 发送场景
     */
    @NotNull(message = "发送场景不能为空")
    @InEnum(SmsSceneEnum.class)
    private Integer scene;

    /**
     * 发送 IP，用于限频
     */
    @NotBlank(message = "发送 IP 不能为空")
    private String createIp;
}
