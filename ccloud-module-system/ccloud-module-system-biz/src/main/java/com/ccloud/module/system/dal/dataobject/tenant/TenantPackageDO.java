package com.ccloud.module.system.dal.dataobject.tenant;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import com.ccloud.framework.mybatis.core.type.JsonLongSetTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * 租户套餐：决定租户可见的菜单范围
 */
@TableName(value = "system_tenant_package", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class TenantPackageDO extends BaseDO {

    @TableId
    private Long id;
    private String name;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    private String remark;
    @TableField(typeHandler = JsonLongSetTypeHandler.class)
    private Set<Long> menuIds;
}
