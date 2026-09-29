package com.ccloud.module.system.api.mail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Data;

import java.io.Serializable;

/**
 * 给单个用户发送邮件请求
 */
@Data
public class MailSendSingleToUserReqDTO implements Serializable {

    /**
     * 用户编号，为空时直接按邮箱发送
     */
    private Long userId;

    /**
     * 邮箱，为空时按用户编号查询
     */
    @Email
    private String mail;

    /**
     * 邮件模板编码
     */
    @NotBlank(message = "邮件模板编码不能为空")
    private String templateCode;

    /**
     * 邮件模板参数
     */
    private Map<String, Object> templateParams;
}
