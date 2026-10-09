package com.ccloud.module.infra.dal.dataobject.db;

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
 * 数据源配置。全局数据，所有租户共享
 */
@TableName(value = "infra_data_source_config", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "password")
@Accessors(chain = true)
public class DataSourceConfigDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 名称
     */
    private String name;
    /**
     * JDBC 连接地址，只支持 MySQL
     */
    private String url;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码，加密存储
     */
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String password;
}
