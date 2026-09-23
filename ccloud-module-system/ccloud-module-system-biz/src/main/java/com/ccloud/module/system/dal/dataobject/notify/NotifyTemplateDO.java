package com.ccloud.module.system.dal.dataobject.notify;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 站内信模板，全局表（各租户共用）
 */
@TableName(value = "system_notify_template", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class NotifyTemplateDO extends BaseDO {

    @TableId
    private Long id;
    private String name;
    /**
     * 模板编码，业务按编码发送
     */
    private String code;
    /**
     * 发送人名称
     */
    private String nickname;
    /**
     * 模板内容，参数写作 {参数名}
     */
    private String content;
    /**
     * 类型，见 NotifyTemplateTypeEnum
     */
    private Integer type;
    /**
     * 参数名，由内容解析得出
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> params;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
    private String remark;
}
