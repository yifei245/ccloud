package com.ccloud.module.system.dal.dataobject.sms;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 短信验证码
 */
@TableName("system_sms_code")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "code")
@Accessors(chain = true)
public class SmsCodeDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String mobile;
    private String code;
    private String createIp;
    /**
     * 发送场景，见 SmsSceneEnum
     */
    private Integer scene;
    /**
     * 该手机号当日发送的第几条，用于每日上限
     */
    private Integer todayIndex;
    private Boolean used;
    private LocalDateTime usedTime;
    private String usedIp;
}
