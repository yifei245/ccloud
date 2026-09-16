package com.ccloud.module.demo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 借用 system_post 表验证 BaseMapperX 在真实 MySQL 上的行为
 */
@TableName("system_post")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class DemoPostDO extends BaseDO {

    @TableId
    private Long id;
    private String code;
    private String name;
    private Integer sort;
    private Integer status;
    private Long tenantId;
}
