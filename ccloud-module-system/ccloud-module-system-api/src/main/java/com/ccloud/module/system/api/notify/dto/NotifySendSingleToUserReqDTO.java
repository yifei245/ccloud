package com.ccloud.module.system.api.notify.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

import java.io.Serializable;

/**
 * 给单个用户发送站内信请求
 */
@Data
public class NotifySendSingleToUserReqDTO implements Serializable {

    /**
     * 用户编号
     */
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    /**
     * 站内信模板编码
     */
    @NotEmpty(message = "站内信模板编码不能为空")
    private String templateCode;

    /**
     * 站内信模板参数
     */
    private Map<String, Object> templateParams;
}
