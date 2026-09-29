package com.ccloud.module.system.dal.dataobject.dept;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 部门，树形结构
 */
@TableName("system_dept")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class DeptDO extends TenantBaseDO {

    public static final Long PARENT_ID_ROOT = 0L;

    @TableId
    private Long id;
    private String name;
    /**
     * 父部门编号，{@link #PARENT_ID_ROOT} 表示顶级
     */
    private Long parentId;
    private Integer sort;
    private Long leaderUserId;
    private String phone;
    private String email;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
}
