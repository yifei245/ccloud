package com.ccloud.module.system.dal.dataobject.user;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import com.ccloud.framework.mybatis.core.type.JsonLongSetTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 管理后台用户
 */
@TableName(value = "system_users", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "password")
@Accessors(chain = true)
public class AdminUserDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String username;
    /**
     * BCrypt 哈希，任何接口都不得返回
     */
    @JsonIgnore
    private String password;
    private String nickname;
    private String remark;
    private Long deptId;
    @TableField(typeHandler = JsonLongSetTypeHandler.class)
    private Set<Long> postIds;
    private String email;
    private String mobile;
    /**
     * 0 未知 1 男 2 女
     */
    private Integer sex;
    private String avatar;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    private String loginIp;
    private LocalDateTime loginDate;
}
