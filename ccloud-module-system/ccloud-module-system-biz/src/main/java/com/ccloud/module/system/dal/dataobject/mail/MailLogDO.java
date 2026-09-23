package com.ccloud.module.system.dal.dataobject.mail;

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
 * 邮件日志：发送前落库，SMTP 发送后回写结果
 */
@TableName(value = "system_mail_log", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MailLogDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    /**
     * 用户类型，见 UserTypeEnum
     */
    private Integer userType;
    private String toMail;
    private Long accountId;
    private String fromMail;

    // ========== 模板快照（渲染后） ==========

    private Long templateId;
    private String templateCode;
    private String templateNickname;
    private String templateTitle;
    private String templateContent;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> templateParams;

    // ========== 发送结果 ==========

    /**
     * 发送状态，见 MailSendStatusEnum
     */
    private Integer sendStatus;
    private LocalDateTime sendTime;
    /**
     * SMTP 返回的 Message-ID
     */
    private String sendMessageId;
    private String sendException;
}
