package com.ccloud.module.system.dal.dataobject.sms;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 短信日志：一条短信的完整生命周期，发送前落库，渠道受理后回写
 */
@TableName(value = "system_sms_log", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class SmsLogDO extends TenantBaseDO {

    @TableId
    private Long id;

    // ========== 渠道与模板快照 ==========

    private Long channelId;
    private String channelCode;
    private Long templateId;
    private String templateCode;
    private Integer templateType;
    /**
     * 渲染后的短信内容
     */
    private String templateContent;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> templateParams;
    private String apiTemplateId;

    // ========== 接收人 ==========

    private String mobile;
    private Long userId;
    /**
     * 用户类型，见 UserTypeEnum
     */
    private Integer userType;

    // ========== 发送结果 ==========

    /**
     * 发送状态，见 SmsSendStatusEnum
     */
    private Integer sendStatus;
    private LocalDateTime sendTime;
    private String apiSendCode;
    private String apiSendMsg;
    private String apiRequestId;
    private String apiSerialNo;

    // ========== 接收结果（渠道回执） ==========

    private Integer receiveStatus;
    private LocalDateTime receiveTime;
    private String apiReceiveCode;
    private String apiReceiveMsg;
}
