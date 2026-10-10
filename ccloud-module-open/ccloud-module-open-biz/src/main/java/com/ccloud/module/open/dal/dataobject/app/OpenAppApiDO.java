package com.ccloud.module.open.dal.dataobject.app;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 开放应用可调用的接口。按租户隔离，随应用所属租户
 */
@TableName("open_app_api")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class OpenAppApiDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 应用编号
     */
    private Long appId;
    /**
     * 接口编号
     */
    private Long apiId;
}
