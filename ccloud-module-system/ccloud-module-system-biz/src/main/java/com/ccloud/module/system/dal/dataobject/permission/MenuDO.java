package com.ccloud.module.system.dal.dataobject.permission;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 菜单：目录、菜单页面、按钮三类，按钮的 permission 即 {@code @ss.hasPermission} 中的标识。
 * 菜单为全局数据，不做租户隔离，租户可见范围由套餐决定
 */
@TableName("system_menu")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MenuDO extends BaseDO {

    public static final Long ID_ROOT = 0L;

    @TableId
    private Long id;
    private String name;
    /**
     * 权限标识，如 system:user:create
     */
    private String permission;
    /**
     * 枚举 {@link com.ccloud.module.system.enums.permission.MenuTypeEnum}
     */
    private Integer type;
    private Integer sort;
    private Long parentId;
    private String path;
    private String icon;
    private String component;
    private String componentName;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    private Boolean visible;
    private Boolean keepAlive;
    private Boolean alwaysShow;
}
