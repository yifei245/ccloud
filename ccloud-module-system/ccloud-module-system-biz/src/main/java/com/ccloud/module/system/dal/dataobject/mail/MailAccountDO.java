package com.ccloud.module.system.dal.dataobject.mail;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import com.ccloud.framework.mybatis.core.type.EncryptTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 邮箱账号，全局表（各租户共用）
 */
@TableName(value = "system_mail_account", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "password")
@Accessors(chain = true)
public class MailAccountDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 发件邮箱
     */
    private String mail;
    private String username;
    /**
     * SMTP 密码或授权码，加密存储
     */
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String password;
    private String host;
    private Integer port;
    private Boolean sslEnable;
    private Boolean starttlsEnable;
}
