package com.ccloud.module.system.dal.dataobject.i18n;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 国际化文案，全局表（各租户共用），覆盖各服务 properties 中的基线文案
 */
@TableName("system_i18n_message")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class I18nMessageDO extends BaseDO {

    /**
     * 来源：系统文案（错误码、界面提示）
     */
    public static final int SOURCE_SYSTEM = 1;

    @TableId
    private Long id;
    /**
     * 语言，如 zh_CN
     */
    private String locale;
    private String code;
    private String content;
    /**
     * 来源：1 系统文案 2 业务文案
     */
    private Integer source;
    /**
     * 业务文案的归属，source=2 时使用
     */
    private String bizType;
    private Long bizId;
    private String field;
}
