package com.ccloud.module.infra.dal.dataobject.config;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 参数配置。全局数据，所有租户共享
 */
@TableName("infra_config")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class ConfigDO extends BaseDO {

    /**
     * 参数主键
     */
    @TableId
    private Long id;
    /**
     * 参数分组
     */
    private String category;
    /**
     * 参数类型，见 ConfigTypeEnum
     */
    private Integer type;
    /**
     * 参数名称
     */
    private String name;
    /**
     * 参数键名，全局唯一
     */
    private String configKey;
    /**
     * 参数键值
     */
    private String value;
    /**
     * 是否可见，不可见的参数不返回给前端
     */
    private Boolean visible;
    /**
     * 备注
     */
    private String remark;
}
