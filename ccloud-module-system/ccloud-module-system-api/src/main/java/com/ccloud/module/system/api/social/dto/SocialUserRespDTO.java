package com.ccloud.module.system.api.social.dto;


import lombok.Data;

import java.io.Serializable;

/**
 * 社交用户
 */
@Data
public class SocialUserRespDTO implements Serializable {

    /**
     * 社交平台 openid
     */
    private String openid;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 关联的本系统用户编号，为空表示尚未绑定
     */
    private Long userId;
}
