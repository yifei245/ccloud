package com.ccloud.module.system.api.social.dto;


import lombok.Data;

import java.io.Serializable;

/**
 * 微信小程序手机号信息
 */
@Data
public class SocialWxPhoneNumberInfoRespDTO implements Serializable {

    /**
     * 带国际区号的手机号
     */
    private String phoneNumber;

    /**
     * 不带区号的手机号
     */
    private String purePhoneNumber;

    /**
     * 国际区号
     */
    private String countryCode;
}
