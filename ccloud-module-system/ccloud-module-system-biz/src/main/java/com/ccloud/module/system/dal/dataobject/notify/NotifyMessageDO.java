package com.ccloud.module.system.dal.dataobject.notify;

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
 * 站内信：一条发给一个用户
 */
@TableName(value = "system_notify_message", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class NotifyMessageDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    /**
     * 用户类型，见 UserTypeEnum
     */
    private Integer userType;

    // ========== 模板快照（渲染后） ==========

    private Long templateId;
    private String templateCode;
    private String templateNickname;
    private String templateContent;
    private Integer templateType;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> templateParams;

    // ========== 阅读 ==========

    private Boolean readStatus;
    private LocalDateTime readTime;
}
