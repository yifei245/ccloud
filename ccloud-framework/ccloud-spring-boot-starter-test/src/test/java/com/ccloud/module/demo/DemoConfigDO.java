package com.ccloud.module.demo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 借用 infra_config 表（唯一索引 uk_config_key (config_key, deleted)）验证逻辑删除与唯一索引的配合
 */
@TableName("infra_config")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class DemoConfigDO extends BaseDO {

    @TableId
    private Long id;
    private String category;
    private Integer type;
    private String name;
    private String configKey;
    private String value;
    private Boolean visible;
}
