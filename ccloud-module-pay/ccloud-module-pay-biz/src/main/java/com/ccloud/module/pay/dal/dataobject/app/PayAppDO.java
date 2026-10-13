package com.ccloud.module.pay.dal.dataobject.app;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 支付应用，每个接入支付的业务方一条。按租户隔离
 */
@TableName("pay_app")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class PayAppDO extends TenantBaseDO {

    /**
     * 应用编号
     */
    @TableId
    private Long id;
    /**
     * 应用标识，租户内唯一，创建后不可修改
     */
    private String appKey;
    /**
     * 应用名
     */
    private String name;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 绑定的开放应用编号。外部系统经开放平台调用支付开放接口、接收支付回调；为空表示不对外开放
     */
    private Long openAppId;
}
