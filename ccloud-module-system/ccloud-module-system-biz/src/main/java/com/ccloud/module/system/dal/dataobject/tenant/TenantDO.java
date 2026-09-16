package com.ccloud.module.system.dal.dataobject.tenant;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 租户。租户表自身不做租户隔离（在 ccloud.tenant.ignore-tables 中）
 */
@TableName("system_tenant")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class TenantDO extends BaseDO {

    /**
     * 套餐编号为 0 表示系统租户：不受套餐限制，不允许修改与删除
     */
    public static final Long PACKAGE_ID_SYSTEM = 0L;

    @TableId
    private Long id;
    private String name;
    /**
     * 联系人的用户编号，即创建租户时自动生成的租户管理员
     */
    private Long contactUserId;
    private String contactName;
    private String contactMobile;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 绑定域名，登录页据此自动识别租户
     */
    private String website;
    private Long packageId;
    private LocalDateTime expireTime;
    /**
     * 账号数量上限
     */
    private Integer accountCount;

    public boolean isSystem() {
        return PACKAGE_ID_SYSTEM.equals(packageId);
    }
}
