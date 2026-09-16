package com.ccloud.module.system.dal.dataobject.permission;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import com.ccloud.framework.mybatis.core.type.JsonLongSetTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * 角色
 */
@TableName(value = "system_role", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class RoleDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String name;
    /**
     * 角色标识，用于 {@code @ss.hasRole('xxx')}
     */
    private String code;
    private Integer sort;
    /**
     * 枚举 {@link com.ccloud.module.system.enums.permission.DataScopeEnum}
     */
    private Integer dataScope;
    /**
     * 数据范围为"指定部门"时的部门编号
     */
    @TableField(typeHandler = JsonLongSetTypeHandler.class)
    private Set<Long> dataScopeDeptIds;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 枚举 {@link com.ccloud.module.system.enums.permission.RoleTypeEnum}
     */
    private Integer type;
    private String remark;
}
