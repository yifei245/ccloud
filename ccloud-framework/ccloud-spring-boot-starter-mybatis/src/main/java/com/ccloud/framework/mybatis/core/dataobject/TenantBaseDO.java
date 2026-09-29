package com.ccloud.framework.mybatis.core.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需要租户隔离的数据对象基类。
 *
 * <p>{@code tenantId} 由多租户拦截器自动写入与过滤，业务代码不应手工赋值——
 * 手工赋值会绕过隔离，是最容易造成跨租户数据泄漏的写法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class TenantBaseDO extends BaseDO {

    @TableField(value = "tenant_id")
    private Long tenantId;
}
