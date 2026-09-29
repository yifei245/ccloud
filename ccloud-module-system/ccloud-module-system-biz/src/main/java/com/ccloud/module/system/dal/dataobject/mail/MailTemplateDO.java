package com.ccloud.module.system.dal.dataobject.mail;

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
 * 邮件模板，全局表（各租户共用）
 */
@TableName(value = "system_mail_template", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MailTemplateDO extends BaseDO {

    @TableId
    private Long id;
    private String name;
    /**
     * 模板编码，业务按编码发送
     */
    private String code;
    /**
     * 发件邮箱账号
     */
    private Long accountId;
    /**
     * 发件人名称
     */
    private String nickname;
    /**
     * 标题，纯文本，参数写作 {参数名}
     */
    private String title;
    /**
     * 内容，HTML，参数写作 {参数名}
     */
    private String content;
    /**
     * 参数名，由标题与内容解析得出
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> params;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
    private String remark;
}
