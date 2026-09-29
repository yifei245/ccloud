package com.ccloud.module.system.api.sms.dto;

import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.framework.common.validation.Mobile;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 使用短信验证码请求（校验通过即标记已使用，不可复用）
 */
@Data
public class SmsCodeUseReqDTO implements Serializable {

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
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;

    /**
     * 使用 IP
     */
    @NotBlank(message = "使用 IP 不能为空")
    private String usedIp;
}
