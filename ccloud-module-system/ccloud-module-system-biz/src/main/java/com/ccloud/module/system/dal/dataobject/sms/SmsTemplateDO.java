package com.ccloud.module.system.dal.dataobject.sms;

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
 * 短信模板，全局表（各租户共用）
 */
@TableName(value = "system_sms_template", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class SmsTemplateDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 模板类型，见 SmsTemplateTypeEnum
     */
    private Integer type;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
    /**
     * 模板编码，业务按编码发送
     */
    private String code;
    private String name;
    /**
     * 模板内容，参数写作 {参数名}
     */
    private String content;
    /**
     * 参数名，按在内容中出现的顺序，由内容解析得出
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> params;
    private String remark;
    /**
     * 渠道侧的模板编号
     */
    private String apiTemplateId;
    private Long channelId;
    /**
     * 冗余渠道编码，避免列表展示时逐条查渠道
     */
    private String channelCode;
}
