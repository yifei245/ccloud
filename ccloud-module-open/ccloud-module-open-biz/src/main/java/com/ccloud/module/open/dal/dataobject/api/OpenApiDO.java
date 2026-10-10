package com.ccloud.module.open.dal.dataobject.api;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 开放接口目录。全局数据，所有租户共享；只有系统租户可以维护
 */
@TableName("open_api")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class OpenApiDO extends BaseDO {

    /**
     * 接口编号
     */
    @TableId
    private Long id;
    /**
     * 接口名称
     */
    private String name;
    /**
     * 接口编码，全局唯一，如 member.user.get
     */
    private String code;
    /**
     * 接口路径，/open-api/ 开头
     */
    private String path;
    /**
     * 请求方法：GET POST PUT DELETE
     */
    private String method;
    /**
     * 接口描述
     */
    private String description;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
}
